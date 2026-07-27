package com.mindcare.emotionservice.assessment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mindcare.emotionservice.assessment.dto.AdminAssessmentResponse;
import com.mindcare.emotionservice.assessment.dto.AdminQuestionResponse;
import com.mindcare.emotionservice.assessment.dto.AnswerOptionResponse;
import com.mindcare.emotionservice.assessment.dto.AssessmentAnswerRequest;
import com.mindcare.emotionservice.assessment.dto.AssessmentDetailResponse;
import com.mindcare.emotionservice.assessment.dto.AssessmentResultResponse;
import com.mindcare.emotionservice.assessment.dto.AssessmentSubmissionRequest;
import com.mindcare.emotionservice.assessment.dto.AssessmentSummaryResponse;
import com.mindcare.emotionservice.assessment.dto.QuestionResponse;
import com.mindcare.emotionservice.assessment.dto.UpsertAssessmentRequest;
import com.mindcare.emotionservice.assessment.dto.UpsertQuestionRequest;
import com.mindcare.emotionservice.assessment.entity.AnswerOptionEntity;
import com.mindcare.emotionservice.assessment.entity.AssessmentCode;
import com.mindcare.emotionservice.assessment.entity.AssessmentEntity;
import com.mindcare.emotionservice.assessment.entity.AssessmentResultEntity;
import com.mindcare.emotionservice.assessment.entity.AssessmentStatus;
import com.mindcare.emotionservice.assessment.entity.QuestionEntity;
import com.mindcare.emotionservice.assessment.mapper.AssessmentMapper;
import com.mindcare.emotionservice.assessment.repository.AnswerOptionRepository;
import com.mindcare.emotionservice.assessment.repository.AssessmentRepository;
import com.mindcare.emotionservice.assessment.repository.AssessmentResultRepository;
import com.mindcare.emotionservice.assessment.repository.QuestionRepository;
import com.mindcare.emotionservice.shared.dto.CursorPageResponse;
import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
import com.mindcare.emotionservice.shared.exception.ResourceConflictException;
import com.mindcare.emotionservice.shared.exception.ResourceNotFoundException;
import com.mindcare.emotionservice.shared.util.CursorCodec;
import com.mindcare.emotionservice.shared.util.RequestHasher;
import com.mindcare.emotionservice.shared.util.ServiceValidator;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AssessmentServiceImpl implements AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final AssessmentResultRepository resultRepository;
    private final AssessmentMapper mapper;
    private final AssessmentDefinitionRegistry assessmentDefinitions;
    private final AssessmentScoringPolicyRegistry scoringPolicies;
    private final CursorCodec cursorCodec;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AssessmentServiceImpl(
            AssessmentRepository assessmentRepository,
            QuestionRepository questionRepository,
            AnswerOptionRepository answerOptionRepository,
            AssessmentResultRepository resultRepository,
            AssessmentMapper mapper,
            AssessmentDefinitionRegistry assessmentDefinitions,
            AssessmentScoringPolicyRegistry scoringPolicies,
            CursorCodec cursorCodec,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.assessmentRepository = assessmentRepository;
        this.questionRepository = questionRepository;
        this.answerOptionRepository = answerOptionRepository;
        this.resultRepository = resultRepository;
        this.mapper = mapper;
        this.assessmentDefinitions = assessmentDefinitions;
        this.scoringPolicies = scoringPolicies;
        this.cursorCodec = cursorCodec;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public List<AssessmentSummaryResponse> getPublishedAssessments() {
        return assessmentRepository.findByStatusAndDeletedAtIsNullOrderByCodeAsc(AssessmentStatus.PUBLISHED)
                .stream()
                .map(mapper::toSummaryResponse)
                .toList();
    }

    @Override
    public AssessmentDetailResponse getPublishedAssessment(AssessmentCode assessmentCode) {
        AssessmentEntity assessment = assessmentRepository
                .findByCodeAndStatusAndDeletedAtIsNull(
                        requireAssessmentCode(assessmentCode),
                        AssessmentStatus.PUBLISHED
                )
                .orElseThrow(() -> new ResourceNotFoundException("Published assessment"));
        return toDetailResponse(assessment);
    }

    @Override
    @Transactional
    public AssessmentResultResponse submitAssessment(
            UUID userId,
            AssessmentCode assessmentCode,
            String idempotencyKey,
            AssessmentSubmissionRequest request
    ) {
        ServiceValidator.requireUserId(userId);
        AssessmentCode code = requireAssessmentCode(assessmentCode);
        String key = ServiceValidator.requireText(idempotencyKey, "idempotencyKey", 255);
        if (request == null || request.assessmentVersion() == null || request.answers() == null) {
            throw new InvalidRequestException("INVALID_SUBMISSION", "version and answers are required");
        }
        AssessmentEntity assessment = assessmentRepository
                .findByCodeAndStatusAndDeletedAtIsNull(code, AssessmentStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Published assessment"));
        if (!assessment.getAssessmentVersion().equals(request.assessmentVersion())) {
            throw new ResourceConflictException(
                    "ASSESSMENT_VERSION_CONFLICT",
                    "The submitted assessment version is not currently published"
            );
        }
        Map<UUID, UUID> submittedAnswers = validateAndIndexAnswers(request.answers());
        String submissionHash = hashSubmission(request);
        var previous = resultRepository.findByUserIdAndAssessment_IdAndIdempotencyKeyAndDeletedAtIsNull(
                userId,
                assessment.getId(),
                key
        );
        if (previous.isPresent()) {
            if (!submissionHash.equals(previous.get().getSubmissionHash())) {
                throw new ResourceConflictException(
                        "IDEMPOTENCY_CONFLICT",
                        "idempotencyKey was already used with different answers"
                );
            }
            return toResultResponse(previous.get());
        }

        List<QuestionEntity> questions = activeQuestions(assessment.getId());
        if (request.answers().size() != questions.size()) {
            throw new InvalidRequestException("INCOMPLETE_ASSESSMENT", "Every question must have exactly one answer");
        }
        ArrayNode answerSnapshot = objectMapper.createArrayNode();
        int totalScore = 0;
        for (QuestionEntity question : questions) {
            UUID selectedOptionId = submittedAnswers.remove(question.getId());
            if (selectedOptionId == null) {
                throw new InvalidRequestException("INCOMPLETE_ASSESSMENT", "A required question is missing");
            }
            AnswerOptionEntity selectedOption = activeOptions(question.getId()).stream()
                    .filter(option -> option.getId().equals(selectedOptionId))
                    .findFirst()
                    .orElseThrow(() -> new InvalidRequestException(
                            "INVALID_ANSWER_OPTION",
                            "Selected option does not belong to the question"
                    ));
            totalScore += selectedOption.getScoreValue();
            ObjectNode snapshotItem = answerSnapshot.addObject();
            snapshotItem.put("questionId", question.getId().toString());
            snapshotItem.put("optionId", selectedOption.getId().toString());
            snapshotItem.put("score", selectedOption.getScoreValue());
        }
        if (!submittedAnswers.isEmpty()) {
            throw new InvalidRequestException("INVALID_QUESTION", "Submission contains a question outside assessment");
        }

        AssessmentScoringPolicyRegistry.ScoringOutcome outcome = scoringPolicies.score(code, totalScore);
        AssessmentResultEntity result = new AssessmentResultEntity(
                userId,
                assessment,
                totalScore,
                outcome.riskLevel(),
                answerSnapshot,
                assessment.getAssessmentVersion(),
                outcome.ruleVersion(),
                key,
                submissionHash,
                outcome.screeningNotice(),
                objectMapper.valueToTree(outcome.recommendations())
        );
        return toResultResponse(resultRepository.saveAndFlush(result));
    }

    @Override
    public CursorPageResponse<AssessmentResultResponse> getAssessmentHistory(
            UUID userId,
            OffsetDateTime from,
            OffsetDateTime to,
            String cursor,
            int limit
    ) {
        ServiceValidator.requireUserId(userId);
        ServiceValidator.validateTimeRange(from, to, 365);
        ServiceValidator.validateLimit(limit);
        String scope = "assessment-result:" + userId + ':' + from + ':' + to;
        CursorCodec.CursorPosition position = cursorCodec.decode(cursor, scope);
        List<AssessmentResultEntity> entities = resultRepository.findHistory(
                userId,
                from,
                to,
                position.timestamp() != null,
                position.timestamp(),
                position.id(),
                PageRequest.of(0, limit + 1)
        );
        boolean hasMore = entities.size() > limit;
        List<AssessmentResultEntity> page = entities.subList(0, Math.min(limit, entities.size()));
        String nextCursor = hasMore
                ? cursorCodec.encode(page.get(page.size() - 1).getCreatedAt(), page.get(page.size() - 1).getId(), scope)
                : null;
        return new CursorPageResponse<>(page.stream().map(this::toResultResponse).toList(), nextCursor, hasMore);
    }

    @Override
    public AssessmentResultResponse getAssessmentResult(UUID userId, UUID resultId) {
        ServiceValidator.requireUserId(userId);
        if (resultId == null) {
            throw new InvalidRequestException("INVALID_REQUEST", "resultId must not be null");
        }
        return resultRepository.findByIdAndUserIdAndDeletedAtIsNull(resultId, userId)
                .map(this::toResultResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment result"));
    }

    @Override
    @Transactional
    public AdminAssessmentResponse createAssessment(UpsertAssessmentRequest request) {
        ValidatedCatalog catalog = validateCatalog(request);
        if (assessmentRepository.findFirstByCodeAndDeletedAtIsNullOrderByAssessmentVersionDesc(catalog.code()).isPresent()) {
            throw new ResourceConflictException("ASSESSMENT_CODE_CONFLICT", "assessment code already exists");
        }
        AssessmentEntity assessment;
        try {
            assessment = assessmentRepository.saveAndFlush(new AssessmentEntity(
                    catalog.code(),
                    catalog.title(),
                    catalog.description(),
                    1
            ));
        } catch (DataIntegrityViolationException exception) {
            throw new ResourceConflictException(
                    "ASSESSMENT_CODE_CONFLICT",
                    "assessment code already exists"
            );
        }
        persistCatalog(assessment, catalog.questions());
        return toAdminResponse(assessment);
    }

    @Override
    @Transactional
    public AdminAssessmentResponse updateAssessment(UUID assessmentId, UpsertAssessmentRequest request) {
        AssessmentEntity existing = findAssessment(assessmentId);
        AssessmentCode requestedCode = requireAssessmentCode(request == null ? null : request.code());
        if (existing.getCode() != requestedCode) {
            throw new ResourceConflictException(
                    "ASSESSMENT_CODE_IMMUTABLE",
                    "assessment code cannot be changed after draft creation"
            );
        }
        ValidatedCatalog catalog = validateCatalog(request);
        if (existing.getStatus() != AssessmentStatus.DRAFT) {
            throw invalidTransition("Only a draft assessment can be updated");
        }

        existing.changeTitle(catalog.title());
        existing.changeDescription(catalog.description());
        replaceCatalog(existing, catalog.questions());
        return toAdminResponse(assessmentRepository.saveAndFlush(existing));
    }

    @Override
    @Transactional
    public AdminAssessmentResponse createNextAssessmentVersion(UUID assessmentId) {
        AssessmentEntity source = findAssessment(assessmentId);
        if (source.getStatus() != AssessmentStatus.PUBLISHED) {
            throw invalidTransition("Only a published assessment can be used to create the next version");
        }

        AssessmentEntity latestVersion = assessmentRepository
                .findFirstByCodeAndDeletedAtIsNullOrderByAssessmentVersionDesc(source.getCode())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment"));
        if (!assessmentRepository.existsByIdAndStatusAndDeletedAtIsNull(
                source.getId(),
                AssessmentStatus.PUBLISHED
        )) {
            throw invalidTransition("Only a published assessment can be used to create the next version");
        }
        if (assessmentRepository.existsByCodeAndStatusAndDeletedAtIsNull(
                source.getCode(),
                AssessmentStatus.DRAFT
        )) {
            throw new ResourceConflictException(
                    "ASSESSMENT_DRAFT_VERSION_EXISTS",
                    "A draft version already exists for this assessment code"
            );
        }

        List<UpsertQuestionRequest> copiedQuestions = activeQuestions(source.getId()).stream()
                .map(question -> new UpsertQuestionRequest(
                        question.getQuestionText(),
                        question.getOrderIndex()
                ))
                .toList();
        ValidatedCatalog catalog = validateCatalog(new UpsertAssessmentRequest(
                source.getCode(),
                source.getTitle(),
                source.getDescription(),
                copiedQuestions
        ));
        AssessmentEntity draft;
        try {
            draft = assessmentRepository.saveAndFlush(new AssessmentEntity(
                    source.getCode(),
                    catalog.title(),
                    catalog.description(),
                    latestVersion.getAssessmentVersion() + 1
            ));
        } catch (DataIntegrityViolationException exception) {
            throw new ResourceConflictException(
                    "ASSESSMENT_VERSION_CONFLICT",
                    "The next assessment version was created concurrently"
            );
        }
        persistCatalog(draft, catalog.questions());
        return toAdminResponse(draft);
    }

    @Override
    @Transactional
    public AdminAssessmentResponse publishAssessment(UUID assessmentId) {
        AssessmentEntity assessment = findAssessment(assessmentId);
        if (assessment.getStatus() != AssessmentStatus.DRAFT) {
            throw invalidTransition("Only a draft assessment can be published");
        }
        validatePublishable(assessment);
        assessmentRepository.findByCodeAndStatusAndDeletedAtIsNull(assessment.getCode(), AssessmentStatus.PUBLISHED)
                .filter(current -> !current.getId().equals(assessment.getId()))
                .ifPresent(current -> {
                    current.archive();
                    assessmentRepository.saveAndFlush(current);
                });
        assessment.publish();
        return toAdminResponse(assessmentRepository.saveAndFlush(assessment));
    }

    @Override
    @Transactional
    public AdminAssessmentResponse archiveAssessment(UUID assessmentId) {
        AssessmentEntity assessment = findAssessment(assessmentId);
        if (assessment.getStatus() != AssessmentStatus.PUBLISHED) {
            throw invalidTransition("Only a published assessment can be archived");
        }
        assessment.archive();
        return toAdminResponse(assessmentRepository.saveAndFlush(assessment));
    }

    @Override
    public AdminAssessmentResponse getAssessmentForAdmin(UUID assessmentId) {
        return toAdminResponse(findAssessment(assessmentId));
    }

    @Override
    public CursorPageResponse<AdminAssessmentResponse> getAssessmentsForAdmin(
            String status,
            String cursor,
            int limit
    ) {
        ServiceValidator.validateLimit(limit);
        AssessmentStatus statusFilter = parseOptionalStatus(status);
        String scope = "assessment-admin:" + (statusFilter == null ? "ALL" : statusFilter.name());
        CursorCodec.CursorPosition position = cursorCodec.decode(cursor, scope);
        List<AssessmentEntity> entities = assessmentRepository.findForAdmin(
                statusFilter != null,
                statusFilter,
                position.timestamp() != null,
                position.timestamp(),
                position.id(),
                PageRequest.of(0, limit + 1)
        );
        boolean hasMore = entities.size() > limit;
        List<AssessmentEntity> page = entities.subList(0, Math.min(limit, entities.size()));
        String nextCursor = hasMore
                ? cursorCodec.encode(page.get(page.size() - 1).getCreatedAt(), page.get(page.size() - 1).getId(), scope)
                : null;
        return new CursorPageResponse<>(page.stream().map(this::toAdminResponse).toList(), nextCursor, hasMore);
    }

    private AssessmentDetailResponse toDetailResponse(AssessmentEntity assessment) {
        List<QuestionResponse> questions = activeQuestions(assessment.getId()).stream()
                .map(question -> mapper.toQuestionResponse(
                        question,
                        activeOptions(question.getId()).stream().map(mapper::toAnswerOptionResponse).toList()
                ))
                .toList();
        return mapper.toDetailResponse(assessment, questions);
    }

    private AdminAssessmentResponse toAdminResponse(AssessmentEntity assessment) {
        List<AdminQuestionResponse> questions = activeQuestions(assessment.getId()).stream()
                .map(question -> mapper.toAdminQuestionResponse(
                        question,
                        activeOptions(question.getId()).stream().map(mapper::toAdminAnswerOptionResponse).toList()
                ))
                .toList();
        return mapper.toAdminResponse(
                assessment,
                questions
        );
    }

    private AssessmentResultResponse toResultResponse(AssessmentResultEntity result) {
        List<String> recommendations = new ArrayList<>();
        if (result.getRecommendations() != null && result.getRecommendations().isArray()) {
            result.getRecommendations().forEach(node -> recommendations.add(node.asText()));
        }
        return mapper.toResultResponse(
                result,
                recommendations
        );
    }

    private void validatePublishable(AssessmentEntity assessment) {
        AssessmentDefinition definition = assessmentDefinitions.getRequired(assessment.getCode());
        List<QuestionEntity> questions = activeQuestions(assessment.getId());
        if (questions.size() != definition.requiredQuestionCount()) {
            throw new InvalidRequestException(
                    "ASSESSMENT_INCOMPLETE",
                    "Assessment requires exactly " + definition.requiredQuestionCount() + " questions before publish"
            );
        }
        for (int questionIndex = 0; questionIndex < questions.size(); questionIndex++) {
            QuestionEntity question = questions.get(questionIndex);
            if (!Integer.valueOf(questionIndex).equals(question.getOrderIndex())) {
                throw new InvalidRequestException(
                        "ASSESSMENT_QUESTION_ORDER_INVALID",
                        "Question order must be contiguous from zero"
                );
            }
            List<AnswerOptionEntity> options = activeOptions(question.getId());
            if (!matchesDefinition(options, definition.responseScale())) {
                throw new InvalidRequestException(
                        "ASSESSMENT_SCALE_MISMATCH",
                        "Answer options do not match the configured assessment scale"
                );
            }
        }
        if (!scoringPolicies.supports(assessment.getCode())) {
            throw new InvalidRequestException(
                    "SCORING_POLICY_NOT_CONFIGURED",
                    "Assessment cannot be published without an approved scoring policy"
            );
        }
    }

    private boolean matchesDefinition(
            List<AnswerOptionEntity> options,
            List<AssessmentDefinition.ResponseScaleOption> expectedScale
    ) {
        if (options.size() != expectedScale.size()) {
            return false;
        }
        for (int index = 0; index < expectedScale.size(); index++) {
            AnswerOptionEntity actual = options.get(index);
            AssessmentDefinition.ResponseScaleOption expected = expectedScale.get(index);
            if (!Integer.valueOf(index).equals(actual.getOrderIndex())
                    || actual.getScoreValue() != expected.rawValue()
                    || !actual.getOptionText().equals(expected.optionText())) {
                return false;
            }
        }
        return true;
    }

    private void replaceCatalog(AssessmentEntity assessment, List<ValidatedQuestion> questions) {
        OffsetDateTime deletedAt = OffsetDateTime.now(clock);
        List<QuestionEntity> existingQuestions = activeQuestions(assessment.getId());
        existingQuestions.forEach(question -> {
            activeOptions(question.getId()).forEach(option -> option.softDelete(deletedAt));
            question.softDelete(deletedAt);
        });
        answerOptionRepository.flush();
        questionRepository.flush();
        persistCatalog(assessment, questions);
    }

    private void persistCatalog(AssessmentEntity assessment, List<ValidatedQuestion> questions) {
        for (ValidatedQuestion questionRequest : questions) {
            QuestionEntity question = questionRepository.saveAndFlush(new QuestionEntity(
                    assessment,
                    questionRequest.questionText(),
                    questionRequest.orderIndex()
            ));
            List<AnswerOptionEntity> options = new ArrayList<>();
            for (int index = 0; index < questionRequest.answerOptions().size(); index++) {
                ValidatedOption option = questionRequest.answerOptions().get(index);
                options.add(new AnswerOptionEntity(question, option.optionText(), option.scoreValue(), index));
            }
            answerOptionRepository.saveAll(options);
        }
        answerOptionRepository.flush();
    }

    private ValidatedCatalog validateCatalog(UpsertAssessmentRequest request) {
        if (request == null || request.questions() == null || request.questions().isEmpty()) {
            throw new InvalidRequestException("INVALID_ASSESSMENT_CATALOG", "assessment questions are required");
        }
        AssessmentCode code = requireAssessmentCode(request.code());
        AssessmentDefinition definition = assessmentDefinitions.getRequired(code);
        if (request.questions().size() > definition.requiredQuestionCount()) {
            throw new InvalidRequestException(
                    "ASSESSMENT_QUESTION_LIMIT_EXCEEDED",
                    "Draft supports at most " + definition.requiredQuestionCount() + " questions"
            );
        }
        String title = ServiceValidator.requireText(request.title(), "title", 255);
        String description = normalizeOptional(request.description());
        Set<Integer> orders = new HashSet<>();
        List<ValidatedQuestion> questions = new ArrayList<>();
        for (UpsertQuestionRequest question : request.questions()) {
            if (question == null || question.orderIndex() == null || question.orderIndex() < 0
                    || !orders.add(question.orderIndex())) {
                throw new InvalidRequestException(
                        "INVALID_ASSESSMENT_CATALOG",
                        "question order must be non-negative and unique"
                );
            }
            questions.add(new ValidatedQuestion(
                    ServiceValidator.requireText(question.questionText(), "questionText", 5_000),
                    question.orderIndex(),
                    definition.responseScale().stream()
                            .map(option -> new ValidatedOption(option.optionText(), option.rawValue()))
                            .toList()
            ));
        }
        questions.sort(java.util.Comparator.comparing(ValidatedQuestion::orderIndex));
        for (int index = 0; index < questions.size(); index++) {
            if (questions.get(index).orderIndex() != index) {
                throw new InvalidRequestException(
                        "ASSESSMENT_QUESTION_ORDER_INVALID",
                        "Question order must be contiguous from zero"
                );
            }
        }
        return new ValidatedCatalog(code, title, description, List.copyOf(questions));
    }

    private Map<UUID, UUID> validateAndIndexAnswers(List<AssessmentAnswerRequest> answers) {
        Map<UUID, UUID> indexed = new HashMap<>();
        for (AssessmentAnswerRequest answer : answers) {
            if (answer == null || answer.questionId() == null || answer.optionId() == null) {
                throw new InvalidRequestException("INVALID_ANSWER", "questionId and optionId are required");
            }
            if (indexed.putIfAbsent(answer.questionId(), answer.optionId()) != null) {
                throw new InvalidRequestException("DUPLICATE_QUESTION", "A question was answered more than once");
            }
        }
        return indexed;
    }

    private List<QuestionEntity> activeQuestions(UUID assessmentId) {
        return questionRepository.findByAssessment_IdAndDeletedAtIsNullOrderByOrderIndexAsc(assessmentId);
    }

    private List<AnswerOptionEntity> activeOptions(UUID questionId) {
        return answerOptionRepository.findByQuestion_IdAndDeletedAtIsNullOrderByOrderIndexAsc(questionId);
    }

    private AssessmentEntity findAssessment(UUID assessmentId) {
        if (assessmentId == null) {
            throw new InvalidRequestException("INVALID_REQUEST", "assessmentId must not be null");
        }
        return assessmentRepository.findByIdAndDeletedAtIsNull(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment"));
    }

    private AssessmentStatus parseOptionalStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return AssessmentStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("INVALID_ASSESSMENT_STATUS", "status is invalid");
        }
    }

    private AssessmentCode requireAssessmentCode(AssessmentCode code) {
        if (code == null) {
            throw new InvalidRequestException(
                    "INVALID_ASSESSMENT_CODE",
                    "assessmentCode must not be null"
            );
        }
        return code;
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String hashSubmission(AssessmentSubmissionRequest request) {
        StringBuilder canonical = new StringBuilder(String.valueOf(request.assessmentVersion()));
        request.answers().stream()
                .sorted(java.util.Comparator.comparing(answer -> answer.questionId().toString()))
                .forEach(answer -> canonical.append('|').append(answer.questionId()).append(':').append(answer.optionId()));
        return RequestHasher.sha256(canonical.toString());
    }

    private ResourceConflictException invalidTransition(String message) {
        return new ResourceConflictException("INVALID_STATE_TRANSITION", message);
    }

    private record ValidatedCatalog(
            AssessmentCode code,
            String title,
            String description,
            List<ValidatedQuestion> questions
    ) {
    }

    private record ValidatedQuestion(
            String questionText,
            Integer orderIndex,
            List<ValidatedOption> answerOptions
    ) {
    }

    private record ValidatedOption(String optionText, Integer scoreValue) {
    }
}
