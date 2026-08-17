package com.mindcare.emotionservice.assessment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindcare.emotionservice.assessment.dto.AdminAssessmentResponse;
import com.mindcare.emotionservice.assessment.dto.AnswerOptionResponse;
import com.mindcare.emotionservice.assessment.dto.AssessmentDetailResponse;
import com.mindcare.emotionservice.assessment.dto.AssessmentSummaryResponse;
import com.mindcare.emotionservice.assessment.dto.QuestionResponse;
import com.mindcare.emotionservice.assessment.dto.AssessmentSubmissionRequest;
import com.mindcare.emotionservice.assessment.dto.AssessmentAnswerRequest;
import com.mindcare.emotionservice.assessment.dto.AssessmentResultResponse;
import com.mindcare.emotionservice.assessment.dto.UpsertAssessmentRequest;
import com.mindcare.emotionservice.assessment.dto.UpsertQuestionRequest;
import com.mindcare.emotionservice.assessment.entity.AnswerOptionEntity;
import com.mindcare.emotionservice.assessment.entity.AssessmentCode;
import com.mindcare.emotionservice.assessment.entity.AssessmentEntity;
import com.mindcare.emotionservice.assessment.entity.AssessmentStatus;
import com.mindcare.emotionservice.assessment.entity.AssessmentResultEntity;
import com.mindcare.emotionservice.assessment.entity.QuestionEntity;
import com.mindcare.emotionservice.assessment.mapper.AssessmentMapper;
import com.mindcare.emotionservice.assessment.repository.AnswerOptionRepository;
import com.mindcare.emotionservice.assessment.repository.AssessmentRepository;
import com.mindcare.emotionservice.assessment.repository.AssessmentResultRepository;
import com.mindcare.emotionservice.assessment.repository.QuestionRepository;
import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
import com.mindcare.emotionservice.shared.exception.ResourceConflictException;
import com.mindcare.emotionservice.shared.exception.ResourceNotFoundException;
import com.mindcare.emotionservice.shared.util.CursorCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceImplTest {

    @Mock
    private AssessmentRepository assessmentRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private AnswerOptionRepository answerOptionRepository;
    @Mock
    private AssessmentResultRepository resultRepository;
    @Mock
    private AssessmentMapper mapper;

    private AssessmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AssessmentServiceImpl(
                assessmentRepository,
                questionRepository,
                answerOptionRepository,
                resultRepository,
                mapper,
                new AssessmentDefinitionRegistry(),
                new AssessmentScoringPolicyRegistry(),
                new CursorCodec(),
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-07-22T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void submitRejectsIncompleteAnswersBeforeScoring() {
        UUID userId = UUID.randomUUID();
        AssessmentEntity assessment = new AssessmentEntity(
                AssessmentCode.PHQ_9,
                "PHQ-9",
                null,
                1
        );
        assessment.publish();
        QuestionEntity question = new QuestionEntity(assessment, "Question", 0);
        when(assessmentRepository.findByCodeAndStatusAndDeletedAtIsNull(
                eq(AssessmentCode.PHQ_9),
                any()
        ))
                .thenReturn(Optional.of(assessment));
        when(resultRepository.findByUserIdAndAssessment_IdAndIdempotencyKeyAndDeletedAtIsNull(
                eq(userId), eq(null), eq("submission-1")
        )).thenReturn(Optional.empty());
        when(questionRepository.findByAssessment_IdAndDeletedAtIsNullOrderByOrderIndexAsc(null))
                .thenReturn(List.of(question));

        assertThrows(InvalidRequestException.class, () -> service.submitAssessment(
                userId,
                AssessmentCode.PHQ_9,
                "submission-1",
                new AssessmentSubmissionRequest(1, List.of())
        ));
    }

    @Test
    void submitScoresServerOwnedOptionAndPersistsTraceableResult() {
        UUID userId = UUID.randomUUID();
        UUID assessmentId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        AssessmentEntity assessment = publishedAssessment(assessmentId);
        QuestionEntity question = question(assessment, questionId);
        AnswerOptionEntity option = option(question, optionId, 2);
        AssessmentSubmissionRequest request = new AssessmentSubmissionRequest(
                1,
                List.of(new AssessmentAnswerRequest(questionId, optionId))
        );
        AssessmentResultResponse expected = new AssessmentResultResponse(
                UUID.randomUUID(),
                AssessmentCode.PHQ_9,
                1,
                2,
                "NORMAL",
                "Screening notice",
                List.of(),
                OffsetDateTime.parse("2026-07-22T00:00:00Z")
        );
        when(assessmentRepository.findByCodeAndStatusAndDeletedAtIsNull(
                AssessmentCode.PHQ_9,
                AssessmentStatus.PUBLISHED
        )).thenReturn(Optional.of(assessment));
        when(resultRepository.findByUserIdAndAssessment_IdAndIdempotencyKeyAndDeletedAtIsNull(
                userId,
                assessmentId,
                "submission-happy"
        )).thenReturn(Optional.empty());
        when(questionRepository.findByAssessment_IdAndDeletedAtIsNullOrderByOrderIndexAsc(assessmentId))
                .thenReturn(List.of(question));
        when(answerOptionRepository.findByQuestion_IdAndDeletedAtIsNullOrderByOrderIndexAsc(questionId))
                .thenReturn(List.of(option));
        when(resultRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResultResponse(
                any(AssessmentResultEntity.class),
                org.mockito.ArgumentMatchers.anyList()
        ))
                .thenReturn(expected);

        AssessmentResultResponse actual = service.submitAssessment(
                userId,
                AssessmentCode.PHQ_9,
                "submission-happy",
                request
        );

        assertSame(expected, actual);
        ArgumentCaptor<AssessmentResultEntity> resultCaptor =
                ArgumentCaptor.forClass(AssessmentResultEntity.class);
        verify(resultRepository).saveAndFlush(resultCaptor.capture());
        AssessmentResultEntity persisted = resultCaptor.getValue();
        assertEquals(userId, persisted.getUserId());
        assertSame(assessment, persisted.getAssessment());
        assertEquals(2, persisted.getTotalScore());
        assertEquals("MINIMAL", persisted.getRiskLevel());
        assertEquals("MINIMAL", persisted.getInterpretationLevel());
        assertEquals("PHQ9_SCORE", persisted.getScoringPolicyKey());
        assertEquals("PHQ9_KROENKE_2001", persisted.getBenchmarkPolicyKey());
        assertEquals(1, persisted.getAssessmentVersion());
        assertEquals("PHQ9_SCORE-1.0", persisted.getScoringRuleVersion());
        assertEquals("submission-happy", persisted.getIdempotencyKey());
        assertNotNull(persisted.getSubmissionHash());
        assertEquals(questionId.toString(), persisted.getAnswersDetail().get(0).get("questionId").asText());
        assertEquals(optionId.toString(), persisted.getAnswersDetail().get(0).get("optionId").asText());
        assertEquals(2, persisted.getAnswersDetail().get(0).get("score").asInt());
    }

    @Test
    void submitReplaysExistingResultForSameIdempotencyKeyAndPayload() {
        UUID userId = UUID.randomUUID();
        UUID assessmentId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        AssessmentEntity assessment = publishedAssessment(assessmentId);
        AssessmentSubmissionRequest request = new AssessmentSubmissionRequest(
                1,
                List.of(new AssessmentAnswerRequest(questionId, optionId))
        );
        String submissionHash = com.mindcare.emotionservice.shared.util.RequestHasher.sha256(
                "1|" + questionId + ":" + optionId
        );
        AssessmentResultEntity existing = new AssessmentResultEntity(
                userId,
                assessment,
                2,
                "NORMAL",
                new ObjectMapper().createArrayNode(),
                1,
                "phq9-v1",
                "submission-replay",
                submissionHash,
                "Screening notice",
                new ObjectMapper().createArrayNode()
        );
        AssessmentResultResponse expected = new AssessmentResultResponse(
                UUID.randomUUID(),
                AssessmentCode.PHQ_9,
                1,
                2,
                "NORMAL",
                "Screening notice",
                List.of(),
                OffsetDateTime.parse("2026-07-22T00:00:00Z")
        );
        when(assessmentRepository.findByCodeAndStatusAndDeletedAtIsNull(
                AssessmentCode.PHQ_9,
                AssessmentStatus.PUBLISHED
        )).thenReturn(Optional.of(assessment));
        when(resultRepository.findByUserIdAndAssessment_IdAndIdempotencyKeyAndDeletedAtIsNull(
                userId,
                assessmentId,
                "submission-replay"
        )).thenReturn(Optional.of(existing));
        when(mapper.toResultResponse(existing, List.of())).thenReturn(expected);

        AssessmentResultResponse actual = service.submitAssessment(
                userId,
                AssessmentCode.PHQ_9,
                "submission-replay",
                request
        );

        assertSame(expected, actual);
        verifyNoInteractions(questionRepository);
        verifyNoInteractions(answerOptionRepository);
        verify(resultRepository, org.mockito.Mockito.never()).saveAndFlush(any());
    }

    @Test
    void submitRejectsReusedIdempotencyKeyWithDifferentPayload() {
        UUID userId = UUID.randomUUID();
        UUID assessmentId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        AssessmentEntity assessment = publishedAssessment(assessmentId);
        AssessmentResultEntity existing = new AssessmentResultEntity(
                userId,
                assessment,
                0,
                "NORMAL",
                new ObjectMapper().createArrayNode(),
                1,
                "phq9-v1",
                "submission-conflict",
                "different-hash",
                "Screening notice",
                new ObjectMapper().createArrayNode()
        );
        when(assessmentRepository.findByCodeAndStatusAndDeletedAtIsNull(
                AssessmentCode.PHQ_9,
                AssessmentStatus.PUBLISHED
        )).thenReturn(Optional.of(assessment));
        when(resultRepository.findByUserIdAndAssessment_IdAndIdempotencyKeyAndDeletedAtIsNull(
                userId,
                assessmentId,
                "submission-conflict"
        )).thenReturn(Optional.of(existing));

        ResourceConflictException exception = assertThrows(
                ResourceConflictException.class,
                () -> service.submitAssessment(
                        userId,
                        AssessmentCode.PHQ_9,
                        "submission-conflict",
                        new AssessmentSubmissionRequest(
                                1,
                                List.of(new AssessmentAnswerRequest(questionId, UUID.randomUUID()))
                        )
                )
        );

        assertEquals("IDEMPOTENCY_CONFLICT", exception.getCode());
        verifyNoInteractions(questionRepository);
        verifyNoInteractions(answerOptionRepository);
    }

    @Test
    void submitRejectsDuplicateQuestionBeforeHashingOrPersistence() {
        UUID userId = UUID.randomUUID();
        UUID assessmentId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        AssessmentEntity assessment = publishedAssessment(assessmentId);
        when(assessmentRepository.findByCodeAndStatusAndDeletedAtIsNull(
                AssessmentCode.PHQ_9,
                AssessmentStatus.PUBLISHED
        )).thenReturn(Optional.of(assessment));

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> service.submitAssessment(
                        userId,
                        AssessmentCode.PHQ_9,
                        "submission-duplicate",
                        new AssessmentSubmissionRequest(
                                1,
                                List.of(
                                        new AssessmentAnswerRequest(questionId, UUID.randomUUID()),
                                        new AssessmentAnswerRequest(questionId, UUID.randomUUID())
                                )
                        )
                )
        );

        assertEquals("DUPLICATE_QUESTION", exception.getCode());
        verifyNoInteractions(resultRepository);
    }

    @Test
    void getAssessmentHistoryReturnsOwnedKeysetPageAndNextCursor() {
        UUID userId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.parse("2026-07-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        AssessmentResultEntity newest = assessmentResult(
                userId,
                UUID.randomUUID(),
                "2026-07-25T10:00:00Z",
                12
        );
        AssessmentResultEntity older = assessmentResult(
                userId,
                UUID.randomUUID(),
                "2026-07-20T09:00:00Z",
                8
        );
        AssessmentResultEntity lookAhead = assessmentResult(
                userId,
                UUID.randomUUID(),
                "2026-07-15T08:00:00Z",
                4
        );
        AssessmentResultResponse newestResponse = resultResponse(
                newest.getId(),
                12,
                newest.getCreatedAt()
        );
        AssessmentResultResponse olderResponse = resultResponse(
                older.getId(),
                8,
                older.getCreatedAt()
        );
        when(resultRepository.findHistory(
                userId,
                from,
                to,
                false,
                null,
                null,
                org.springframework.data.domain.PageRequest.of(0, 3)
        )).thenReturn(List.of(newest, older, lookAhead));
        when(mapper.toResultResponse(newest, List.of())).thenReturn(newestResponse);
        when(mapper.toResultResponse(older, List.of())).thenReturn(olderResponse);

        var page = service.getAssessmentHistory(userId, from, to, null, 2);

        assertEquals(List.of(newestResponse, olderResponse), page.items());
        assertEquals(true, page.hasMore());
        assertNotNull(page.nextCursor());
        CursorCodec.CursorPosition cursorPosition = new CursorCodec().decode(
                page.nextCursor(),
                "assessment-result:" + userId + ':' + from + ':' + to
        );
        assertEquals(older.getCreatedAt(), cursorPosition.timestamp());
        assertEquals(older.getId(), cursorPosition.id());
    }

    @Test
    void getAssessmentHistoryRejectsRangeLongerThanOneYearBeforeRepositoryCall() {
        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> service.getAssessmentHistory(
                        UUID.randomUUID(),
                        OffsetDateTime.parse("2025-01-01T00:00:00Z"),
                        OffsetDateTime.parse("2026-01-02T00:00:00Z"),
                        null,
                        20
                )
        );

        assertEquals("INVALID_TIME_RANGE", exception.getCode());
        verifyNoInteractions(resultRepository);
    }

    @Test
    void getAssessmentResultReturnsOnlyOwnedActiveResult() {
        UUID userId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();
        AssessmentResultEntity entity = assessmentResult(
                userId,
                resultId,
                "2026-07-25T10:00:00Z",
                8
        );
        AssessmentResultResponse expected = resultResponse(
                resultId,
                8,
                entity.getCreatedAt()
        );
        when(resultRepository.findByIdAndUserIdAndDeletedAtIsNull(resultId, userId))
                .thenReturn(Optional.of(entity));
        when(mapper.toResultResponse(entity, List.of())).thenReturn(expected);

        AssessmentResultResponse actual = service.getAssessmentResult(userId, resultId);

        assertSame(expected, actual);
        verify(resultRepository).findByIdAndUserIdAndDeletedAtIsNull(resultId, userId);
    }

    @Test
    void getAssessmentResultHidesMissingDeletedOrForeignResultAsNotFound() {
        UUID userId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();
        when(resultRepository.findByIdAndUserIdAndDeletedAtIsNull(resultId, userId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.getAssessmentResult(userId, resultId)
        );

        verifyNoInteractions(mapper);
    }

    @Test
    void getPublishedAssessmentsReturnsRepositoryOrderAsSummaries() {
        AssessmentEntity gad7 = new AssessmentEntity(
                AssessmentCode.GAD_7,
                "GAD-7",
                "Anxiety screening",
                1
        );
        gad7.publish();
        AssessmentEntity phq9 = new AssessmentEntity(
                AssessmentCode.PHQ_9,
                "PHQ-9",
                "Mood screening",
                2
        );
        phq9.publish();
        when(assessmentRepository.findByStatusAndDeletedAtIsNullOrderByCodeAsc(
                AssessmentStatus.PUBLISHED
        )).thenReturn(List.of(gad7, phq9));
        List<AssessmentSummaryResponse> result = service.getPublishedAssessments();

        assertEquals(List.of(AssessmentCode.GAD_7, AssessmentCode.PHQ_9),
                result.stream().map(AssessmentSummaryResponse::code).toList());
        assertNotNull(result.get(0).evidence());
        verify(assessmentRepository)
                .findByStatusAndDeletedAtIsNullOrderByCodeAsc(AssessmentStatus.PUBLISHED);
    }

    @Test
    void getPublishedAssessmentReturnsOrderedQuestionsAndOptionsWithoutScores() {
        AssessmentEntity assessment = new AssessmentEntity(
                AssessmentCode.PHQ_9,
                "PHQ-9",
                "Mood screening",
                2
        );
        assessment.publish();
        QuestionEntity question = new QuestionEntity(assessment, "Question 1", 0);
        AnswerOptionEntity option = new AnswerOptionEntity(question, "Không hề", 0, 0);
        AnswerOptionResponse optionResponse = new AnswerOptionResponse(null, "Không hề");
        QuestionResponse questionResponse = new QuestionResponse(
                null,
                "Question 1",
                0,
                List.of(optionResponse)
        );
        AssessmentDetailResponse expected = new AssessmentDetailResponse(
                null,
                AssessmentCode.PHQ_9,
                2,
                "PHQ-9",
                "Mood screening",
                List.of(questionResponse)
        );
        when(assessmentRepository.findByCodeAndStatusAndDeletedAtIsNull(
                AssessmentCode.PHQ_9,
                AssessmentStatus.PUBLISHED
        )).thenReturn(Optional.of(assessment));
        when(questionRepository.findByAssessment_IdAndDeletedAtIsNullOrderByOrderIndexAsc(null))
                .thenReturn(List.of(question));
        when(answerOptionRepository.findByQuestion_IdAndDeletedAtIsNullOrderByOrderIndexAsc(null))
                .thenReturn(List.of(option));
        when(mapper.toAnswerOptionResponse(option)).thenReturn(optionResponse);
        when(mapper.toQuestionResponse(question, List.of(optionResponse))).thenReturn(questionResponse);
        AssessmentDetailResponse result =
                service.getPublishedAssessment(AssessmentCode.PHQ_9);

        assertEquals(expected.code(), result.code());
        assertEquals(expected.questions(), result.questions());
        assertNotNull(result.evidence());
    }

    @Test
    void getPublishedAssessmentHidesDraftOrArchivedVersionAsNotFound() {
        when(assessmentRepository.findByCodeAndStatusAndDeletedAtIsNull(
                AssessmentCode.WHO_5,
                AssessmentStatus.PUBLISHED
        )).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.getPublishedAssessment(AssessmentCode.WHO_5)
        );

        verifyNoInteractions(questionRepository);
        verifyNoInteractions(answerOptionRepository);
    }

    @Test
    void publishRejectsPhq9DraftWithFewerThanNineQuestions() {
        UUID assessmentId = UUID.randomUUID();
        AssessmentEntity assessment = new AssessmentEntity(
                AssessmentCode.PHQ_9,
                "PHQ-9",
                null,
                1
        );
        when(assessmentRepository.findByIdAndDeletedAtIsNull(assessmentId)).thenReturn(Optional.of(assessment));
        when(questionRepository.findByAssessment_IdAndDeletedAtIsNullOrderByOrderIndexAsc(null))
                .thenReturn(java.util.stream.IntStream.range(0, 8)
                        .mapToObj(index -> new QuestionEntity(assessment, "Question " + index, index))
                        .toList());

        InvalidRequestException exception =
                assertThrows(InvalidRequestException.class, () -> service.publishAssessment(assessmentId));

        assertEquals("ASSESSMENT_INCOMPLETE", exception.getCode());
    }

    @Test
    void publishAcceptsPhq9DraftWithExactlyNineQuestionsAndCanonicalScale() {
        UUID assessmentId = UUID.randomUUID();
        AssessmentEntity assessment = new AssessmentEntity(
                AssessmentCode.PHQ_9,
                "PHQ-9",
                null,
                1
        );
        List<QuestionEntity> questions = java.util.stream.IntStream.range(0, 9)
                .mapToObj(index -> new QuestionEntity(assessment, "Question " + index, index))
                .toList();
        QuestionEntity optionOwner = questions.get(0);
        List<AnswerOptionEntity> options = List.of(
                new AnswerOptionEntity(optionOwner, "Không hề", 0, 0),
                new AnswerOptionEntity(optionOwner, "Vài ngày", 1, 1),
                new AnswerOptionEntity(optionOwner, "Hơn một nửa số ngày", 2, 2),
                new AnswerOptionEntity(optionOwner, "Gần như mỗi ngày", 3, 3)
        );
        when(assessmentRepository.findByIdAndDeletedAtIsNull(assessmentId)).thenReturn(Optional.of(assessment));
        when(questionRepository.findByAssessment_IdAndDeletedAtIsNullOrderByOrderIndexAsc(null))
                .thenReturn(questions);
        when(answerOptionRepository.findByQuestion_IdAndDeletedAtIsNullOrderByOrderIndexAsc(null))
                .thenReturn(options);
        when(assessmentRepository.saveAndFlush(assessment)).thenReturn(assessment);

        service.publishAssessment(assessmentId);

        assertEquals(AssessmentStatus.PUBLISHED, assessment.getStatus());
        verify(assessmentRepository).saveAndFlush(assessment);
    }

    @Test
    void publishRejectsCatalogWhoseOptionsDoNotMatchDefinition() {
        UUID assessmentId = UUID.randomUUID();
        AssessmentEntity assessment = new AssessmentEntity(
                AssessmentCode.PHQ_9,
                "PHQ-9",
                null,
                1
        );
        List<QuestionEntity> questions = java.util.stream.IntStream.range(0, 9)
                .mapToObj(index -> new QuestionEntity(assessment, "Question " + index, index))
                .toList();
        when(assessmentRepository.findByIdAndDeletedAtIsNull(assessmentId)).thenReturn(Optional.of(assessment));
        when(questionRepository.findByAssessment_IdAndDeletedAtIsNullOrderByOrderIndexAsc(null))
                .thenReturn(questions);
        when(answerOptionRepository.findByQuestion_IdAndDeletedAtIsNullOrderByOrderIndexAsc(null))
                .thenReturn(List.of(
                        new AnswerOptionEntity(questions.get(0), "Manual option", 0, 0),
                        new AnswerOptionEntity(questions.get(0), "Manual option", 1, 1)
                ));

        InvalidRequestException exception =
                assertThrows(InvalidRequestException.class, () -> service.publishAssessment(assessmentId));

        assertEquals("ASSESSMENT_SCALE_MISMATCH", exception.getCode());
        assertEquals(AssessmentStatus.DRAFT, assessment.getStatus());
    }

    @Test
    void publishRejectsAssessmentThatIsNotDraft() {
        UUID assessmentId = UUID.randomUUID();
        AssessmentEntity assessment = new AssessmentEntity(
                AssessmentCode.PHQ_9,
                "PHQ-9",
                null,
                1
        );
        assessment.publish();
        when(assessmentRepository.findByIdAndDeletedAtIsNull(assessmentId))
                .thenReturn(Optional.of(assessment));

        ResourceConflictException exception = assertThrows(
                ResourceConflictException.class,
                () -> service.publishAssessment(assessmentId)
        );

        assertEquals("INVALID_STATE_TRANSITION", exception.getCode());
        verifyNoInteractions(questionRepository);
        verifyNoInteractions(answerOptionRepository);
    }

    @Test
    void archiveAcceptsPublishedAssessment() {
        UUID assessmentId = UUID.randomUUID();
        AssessmentEntity assessment = new AssessmentEntity(
                AssessmentCode.GAD_7,
                "GAD-7",
                null,
                1
        );
        assessment.publish();
        when(assessmentRepository.findByIdAndDeletedAtIsNull(assessmentId))
                .thenReturn(Optional.of(assessment));
        when(assessmentRepository.saveAndFlush(assessment)).thenReturn(assessment);

        service.archiveAssessment(assessmentId);

        assertEquals(AssessmentStatus.ARCHIVED, assessment.getStatus());
        verify(assessmentRepository).saveAndFlush(assessment);
    }

    @Test
    void archiveRejectsDraftAssessment() {
        UUID assessmentId = UUID.randomUUID();
        AssessmentEntity assessment = new AssessmentEntity(
                AssessmentCode.GAD_7,
                "GAD-7",
                null,
                1
        );
        when(assessmentRepository.findByIdAndDeletedAtIsNull(assessmentId))
                .thenReturn(Optional.of(assessment));

        ResourceConflictException exception = assertThrows(
                ResourceConflictException.class,
                () -> service.archiveAssessment(assessmentId)
        );

        assertEquals("INVALID_STATE_TRANSITION", exception.getCode());
        assertEquals(AssessmentStatus.DRAFT, assessment.getStatus());
    }

    @Test
    void archiveRejectsAssessmentThatIsAlreadyArchived() {
        UUID assessmentId = UUID.randomUUID();
        AssessmentEntity assessment = new AssessmentEntity(
                AssessmentCode.GAD_7,
                "GAD-7",
                null,
                1
        );
        assessment.publish();
        assessment.archive();
        when(assessmentRepository.findByIdAndDeletedAtIsNull(assessmentId))
                .thenReturn(Optional.of(assessment));

        ResourceConflictException exception = assertThrows(
                ResourceConflictException.class,
                () -> service.archiveAssessment(assessmentId)
        );

        assertEquals("INVALID_STATE_TRANSITION", exception.getCode());
        assertEquals(AssessmentStatus.ARCHIVED, assessment.getStatus());
    }

    @Test
    void getAssessmentForAdminReturnsAggregateRegardlessOfLifecycleStatus() {
        UUID assessmentId = UUID.randomUUID();
        AssessmentEntity assessment = new AssessmentEntity(
                AssessmentCode.PHQ_9,
                "PHQ-9",
                "Description",
                2
        );
        assessment.publish();
        assessment.archive();
        AdminAssessmentResponse expected = new AdminAssessmentResponse(
                null,
                AssessmentCode.PHQ_9,
                2,
                "ARCHIVED",
                "PHQ-9",
                "Description",
                List.of(),
                null,
                null
        );
        when(assessmentRepository.findByIdAndDeletedAtIsNull(assessmentId))
                .thenReturn(Optional.of(assessment));
        when(questionRepository.findByAssessment_IdAndDeletedAtIsNullOrderByOrderIndexAsc(null))
                .thenReturn(List.of());
        when(mapper.toAdminResponse(assessment, List.of())).thenReturn(expected);

        AdminAssessmentResponse result = service.getAssessmentForAdmin(assessmentId);

        assertSame(expected, result);
        verify(assessmentRepository).findByIdAndDeletedAtIsNull(assessmentId);
    }

    @Test
    void getAssessmentForAdminRejectsMissingOrSoftDeletedAssessment() {
        UUID assessmentId = UUID.randomUUID();
        when(assessmentRepository.findByIdAndDeletedAtIsNull(assessmentId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.getAssessmentForAdmin(assessmentId)
        );

        verifyNoInteractions(questionRepository);
        verifyNoInteractions(answerOptionRepository);
    }

    @Test
    void createNextVersionCopiesPublishedCatalogIntoIncrementedDraft() {
        UUID sourceId = UUID.randomUUID();
        AssessmentEntity source = new AssessmentEntity(
                AssessmentCode.PHQ_9,
                "PHQ-9",
                "Published description",
                1
        );
        source.publish();
        List<QuestionEntity> sourceQuestions = java.util.stream.IntStream.range(0, 9)
                .mapToObj(index -> new QuestionEntity(source, "Question " + index, index))
                .toList();
        AdminAssessmentResponse expected = new AdminAssessmentResponse(
                null,
                AssessmentCode.PHQ_9,
                2,
                "DRAFT",
                "PHQ-9",
                "Published description",
                List.of(),
                null,
                null
        );
        when(assessmentRepository.findByIdAndDeletedAtIsNull(sourceId))
                .thenReturn(Optional.of(source));
        when(assessmentRepository.findFirstByCodeAndDeletedAtIsNullOrderByAssessmentVersionDesc(
                AssessmentCode.PHQ_9
        )).thenReturn(Optional.of(source));
        when(assessmentRepository.existsByIdAndStatusAndDeletedAtIsNull(
                null,
                AssessmentStatus.PUBLISHED
        )).thenReturn(true);
        when(questionRepository.findByAssessment_IdAndDeletedAtIsNullOrderByOrderIndexAsc(null))
                .thenReturn(sourceQuestions, List.of());
        when(assessmentRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toAdminResponse(any(), eq(List.of()))).thenReturn(expected);

        AdminAssessmentResponse result = service.createNextAssessmentVersion(sourceId);

        ArgumentCaptor<AssessmentEntity> assessmentCaptor =
                ArgumentCaptor.forClass(AssessmentEntity.class);
        verify(assessmentRepository).saveAndFlush(assessmentCaptor.capture());
        AssessmentEntity draft = assessmentCaptor.getValue();
        assertEquals(AssessmentCode.PHQ_9, draft.getCode());
        assertEquals(2, draft.getAssessmentVersion());
        assertEquals(AssessmentStatus.DRAFT, draft.getStatus());
        assertEquals("PHQ-9", draft.getTitle());
        assertEquals("Published description", draft.getDescription());
        verify(questionRepository, org.mockito.Mockito.times(9)).saveAndFlush(any());
        verify(answerOptionRepository, org.mockito.Mockito.times(9)).saveAll(any());
        assertSame(expected, result);
        assertEquals(AssessmentStatus.PUBLISHED, source.getStatus());
    }

    @Test
    void createNextVersionRejectsSourceThatIsNotPublished() {
        UUID sourceId = UUID.randomUUID();
        AssessmentEntity source = new AssessmentEntity(
                AssessmentCode.PHQ_9,
                "PHQ-9",
                null,
                1
        );
        when(assessmentRepository.findByIdAndDeletedAtIsNull(sourceId))
                .thenReturn(Optional.of(source));

        ResourceConflictException exception = assertThrows(
                ResourceConflictException.class,
                () -> service.createNextAssessmentVersion(sourceId)
        );

        assertEquals("INVALID_STATE_TRANSITION", exception.getCode());
        verifyNoInteractions(questionRepository);
        verifyNoInteractions(answerOptionRepository);
    }

    @Test
    void createNextVersionRejectsWhenDraftAlreadyExists() {
        UUID sourceId = UUID.randomUUID();
        AssessmentEntity source = new AssessmentEntity(
                AssessmentCode.GAD_7,
                "GAD-7",
                null,
                1
        );
        source.publish();
        AssessmentEntity existingDraft = new AssessmentEntity(
                AssessmentCode.GAD_7,
                "GAD-7 v2",
                null,
                2
        );
        when(assessmentRepository.findByIdAndDeletedAtIsNull(sourceId))
                .thenReturn(Optional.of(source));
        when(assessmentRepository.findFirstByCodeAndDeletedAtIsNullOrderByAssessmentVersionDesc(
                AssessmentCode.GAD_7
        )).thenReturn(Optional.of(existingDraft));
        when(assessmentRepository.existsByIdAndStatusAndDeletedAtIsNull(
                null,
                AssessmentStatus.PUBLISHED
        )).thenReturn(true);
        when(assessmentRepository.existsByCodeAndStatusAndDeletedAtIsNull(
                AssessmentCode.GAD_7,
                AssessmentStatus.DRAFT
        )).thenReturn(true);

        ResourceConflictException exception = assertThrows(
                ResourceConflictException.class,
                () -> service.createNextAssessmentVersion(sourceId)
        );

        assertEquals("ASSESSMENT_DRAFT_VERSION_EXISTS", exception.getCode());
        verifyNoInteractions(questionRepository);
        verifyNoInteractions(answerOptionRepository);
    }

    @Test
    void createNextVersionRejectsSourceArchivedBeforeLatestVersionLock() {
        UUID sourceId = UUID.randomUUID();
        AssessmentEntity source = new AssessmentEntity(
                AssessmentCode.GAD_7,
                "GAD-7",
                null,
                1
        );
        source.publish();
        when(assessmentRepository.findByIdAndDeletedAtIsNull(sourceId))
                .thenReturn(Optional.of(source));
        when(assessmentRepository.findFirstByCodeAndDeletedAtIsNullOrderByAssessmentVersionDesc(
                AssessmentCode.GAD_7
        )).thenReturn(Optional.of(source));

        ResourceConflictException exception = assertThrows(
                ResourceConflictException.class,
                () -> service.createNextAssessmentVersion(sourceId)
        );

        assertEquals("INVALID_STATE_TRANSITION", exception.getCode());
        verifyNoInteractions(questionRepository);
        verifyNoInteractions(answerOptionRepository);
    }

    @Test
    void createNextVersionMapsConcurrentUniqueViolationToStableConflict() {
        UUID sourceId = UUID.randomUUID();
        AssessmentEntity source = new AssessmentEntity(
                AssessmentCode.PHQ_9,
                "PHQ-9",
                null,
                1
        );
        source.publish();
        List<QuestionEntity> sourceQuestions = java.util.stream.IntStream.range(0, 9)
                .mapToObj(index -> new QuestionEntity(source, "Question " + index, index))
                .toList();
        when(assessmentRepository.findByIdAndDeletedAtIsNull(sourceId))
                .thenReturn(Optional.of(source));
        when(assessmentRepository.findFirstByCodeAndDeletedAtIsNullOrderByAssessmentVersionDesc(
                AssessmentCode.PHQ_9
        )).thenReturn(Optional.of(source));
        when(assessmentRepository.existsByIdAndStatusAndDeletedAtIsNull(
                null,
                AssessmentStatus.PUBLISHED
        )).thenReturn(true);
        when(questionRepository.findByAssessment_IdAndDeletedAtIsNullOrderByOrderIndexAsc(null))
                .thenReturn(sourceQuestions);
        when(assessmentRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("unique violation"));

        ResourceConflictException exception = assertThrows(
                ResourceConflictException.class,
                () -> service.createNextAssessmentVersion(sourceId)
        );

        assertEquals("ASSESSMENT_VERSION_CONFLICT", exception.getCode());
        verifyNoInteractions(answerOptionRepository);
    }

    @Test
    void createAssessmentPersistsDraftQuestionAndOrderedAnswerOptions() {
        UpsertAssessmentRequest request = new UpsertAssessmentRequest(
                AssessmentCode.WHO_5,
                "WHO-5",
                "Well-being screening",
                List.of(new UpsertQuestionRequest(
                        "I have felt cheerful and in good spirits",
                        0
                ))
        );
        AdminAssessmentResponse expected = new AdminAssessmentResponse(
                null,
                AssessmentCode.WHO_5,
                1,
                "DRAFT",
                "WHO-5",
                "Well-being screening",
                List.of(),
                (OffsetDateTime) null,
                null
        );
        when(assessmentRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionRepository.findByAssessment_IdAndDeletedAtIsNullOrderByOrderIndexAsc(null))
                .thenReturn(List.of());
        when(mapper.toAdminResponse(any(), eq(List.of()))).thenReturn(expected);

        AdminAssessmentResponse result = service.createAssessment(request);

        ArgumentCaptor<AssessmentEntity> assessmentCaptor =
                ArgumentCaptor.forClass(AssessmentEntity.class);
        verify(assessmentRepository).saveAndFlush(assessmentCaptor.capture());
        AssessmentEntity assessment = assessmentCaptor.getValue();
        assertEquals(AssessmentCode.WHO_5, assessment.getCode());
        assertEquals(1, assessment.getAssessmentVersion());
        assertEquals(AssessmentStatus.DRAFT, assessment.getStatus());

        ArgumentCaptor<QuestionEntity> questionCaptor =
                ArgumentCaptor.forClass(QuestionEntity.class);
        verify(questionRepository).saveAndFlush(questionCaptor.capture());
        QuestionEntity question = questionCaptor.getValue();
        assertSame(assessment, question.getAssessment());
        assertEquals(0, question.getOrderIndex());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<AnswerOptionEntity>> optionCaptor =
                ArgumentCaptor.forClass(Iterable.class);
        verify(answerOptionRepository).saveAll(optionCaptor.capture());
        List<AnswerOptionEntity> options = new java.util.ArrayList<>();
        optionCaptor.getValue().forEach(options::add);
        assertEquals(6, options.size());
        assertSame(question, options.get(0).getQuestion());
        assertEquals(0, options.get(0).getOrderIndex());
        assertEquals(0, options.get(0).getScoreValue());
        assertEquals("Không lúc nào", options.get(0).getOptionText());
        assertEquals(5, options.get(5).getOrderIndex());
        assertEquals(5, options.get(5).getScoreValue());
        assertEquals("Mọi lúc", options.get(5).getOptionText());
        verify(answerOptionRepository).flush();
        assertSame(expected, result);
    }

    @Test
    void createAssessmentRejectsDuplicateQuestionOrderBeforePersistence() {
        UpsertQuestionRequest first = new UpsertQuestionRequest(
                "Question 1",
                0
        );
        UpsertQuestionRequest duplicateOrder = new UpsertQuestionRequest(
                "Question 2",
                0
        );

        assertThrows(
                InvalidRequestException.class,
                () -> service.createAssessment(new UpsertAssessmentRequest(
                        AssessmentCode.PSS_10,
                        "PSS-10",
                        null,
                        List.of(first, duplicateOrder)
                ))
        );

        verifyNoInteractions(assessmentRepository);
        verifyNoInteractions(questionRepository);
        verifyNoInteractions(answerOptionRepository);
    }

    @Test
    void createAssessmentRejectsNonContiguousQuestionOrder() {
        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> service.createAssessment(new UpsertAssessmentRequest(
                        AssessmentCode.WHO_5,
                        "WHO-5",
                        null,
                        List.of(
                                new UpsertQuestionRequest("Question 1", 0),
                                new UpsertQuestionRequest("Question 2", 2)
                        )
                ))
        );

        assertEquals("ASSESSMENT_QUESTION_ORDER_INVALID", exception.getCode());
        verifyNoInteractions(assessmentRepository);
    }

    @Test
    void createAssessmentRejectsMoreQuestionsThanDefinitionAllows() {
        List<UpsertQuestionRequest> questions = java.util.stream.IntStream.range(0, 6)
                .mapToObj(index -> new UpsertQuestionRequest("Question " + index, index))
                .toList();

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> service.createAssessment(new UpsertAssessmentRequest(
                        AssessmentCode.WHO_5,
                        "WHO-5",
                        null,
                        questions
                ))
        );

        assertEquals("ASSESSMENT_QUESTION_LIMIT_EXCEEDED", exception.getCode());
        verifyNoInteractions(assessmentRepository);
    }

    @Test
    void createAssessmentMapsConcurrentUniqueViolationToStableConflict() {
        UpsertAssessmentRequest request = new UpsertAssessmentRequest(
                AssessmentCode.GAD_7,
                "GAD-7",
                null,
                List.of(new UpsertQuestionRequest(
                        "Question",
                        0
                ))
        );
        when(assessmentRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("unique violation"));

        ResourceConflictException exception = assertThrows(
                ResourceConflictException.class,
                () -> service.createAssessment(request)
        );

        assertEquals("ASSESSMENT_CODE_CONFLICT", exception.getCode());
        verifyNoInteractions(questionRepository);
        verifyNoInteractions(answerOptionRepository);
    }

    @Test
    void updateAssessmentReplacesDraftCatalogWithoutChangingIdentityOrVersion() {
        UUID assessmentId = UUID.randomUUID();
        AssessmentEntity assessment = new AssessmentEntity(
                AssessmentCode.GAD_7,
                "Old title",
                "Old description",
                1
        );
        QuestionEntity oldQuestion = new QuestionEntity(assessment, "Old question", 0);
        AnswerOptionEntity oldOption = new AnswerOptionEntity(oldQuestion, "Old option", 0, 0);
        UpsertAssessmentRequest request = new UpsertAssessmentRequest(
                AssessmentCode.GAD_7,
                "Updated title",
                "Updated description",
                List.of(new UpsertQuestionRequest(
                        "Updated question",
                        0
                ))
        );
        AdminAssessmentResponse expected = new AdminAssessmentResponse(
                null,
                AssessmentCode.GAD_7,
                1,
                "DRAFT",
                "Updated title",
                "Updated description",
                List.of(),
                null,
                null
        );
        when(assessmentRepository.findByIdAndDeletedAtIsNull(assessmentId))
                .thenReturn(Optional.of(assessment));
        when(questionRepository.findByAssessment_IdAndDeletedAtIsNullOrderByOrderIndexAsc(null))
                .thenReturn(List.of(oldQuestion), List.of());
        when(answerOptionRepository.findByQuestion_IdAndDeletedAtIsNullOrderByOrderIndexAsc(null))
                .thenReturn(List.of(oldOption));
        when(assessmentRepository.saveAndFlush(assessment)).thenReturn(assessment);
        when(questionRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toAdminResponse(assessment, List.of())).thenReturn(expected);

        AdminAssessmentResponse result = service.updateAssessment(assessmentId, request);

        assertSame(expected, result);
        assertEquals("Updated title", assessment.getTitle());
        assertEquals("Updated description", assessment.getDescription());
        assertEquals(AssessmentCode.GAD_7, assessment.getCode());
        assertEquals(1, assessment.getAssessmentVersion());
        assertEquals(AssessmentStatus.DRAFT, assessment.getStatus());
        assertNotNull(oldQuestion.getDeletedAt());
        assertNotNull(oldOption.getDeletedAt());

        ArgumentCaptor<QuestionEntity> questionCaptor =
                ArgumentCaptor.forClass(QuestionEntity.class);
        verify(questionRepository).saveAndFlush(questionCaptor.capture());
        QuestionEntity replacement = questionCaptor.getValue();
        assertSame(assessment, replacement.getAssessment());
        assertEquals("Updated question", replacement.getQuestionText());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<AnswerOptionEntity>> optionCaptor =
                ArgumentCaptor.forClass(Iterable.class);
        verify(answerOptionRepository).saveAll(optionCaptor.capture());
        List<AnswerOptionEntity> replacementOptions = new java.util.ArrayList<>();
        optionCaptor.getValue().forEach(replacementOptions::add);
        assertEquals(4, replacementOptions.size());
        assertEquals(0, replacementOptions.get(0).getOrderIndex());
        assertEquals("Không hề", replacementOptions.get(0).getOptionText());
        assertEquals(3, replacementOptions.get(3).getOrderIndex());
        assertEquals("Gần như mỗi ngày", replacementOptions.get(3).getOptionText());
    }

    @Test
    void updateAssessmentRejectsPublishedVersion() {
        UUID assessmentId = UUID.randomUUID();
        AssessmentEntity assessment = new AssessmentEntity(
                AssessmentCode.PHQ_9,
                "PHQ-9",
                null,
                1
        );
        assessment.publish();
        when(assessmentRepository.findByIdAndDeletedAtIsNull(assessmentId))
                .thenReturn(Optional.of(assessment));

        ResourceConflictException exception = assertThrows(
                ResourceConflictException.class,
                () -> service.updateAssessment(assessmentId, validUpdateRequest(AssessmentCode.PHQ_9))
        );

        assertEquals("INVALID_STATE_TRANSITION", exception.getCode());
        verifyNoInteractions(questionRepository);
        verifyNoInteractions(answerOptionRepository);
    }

    @Test
    void updateAssessmentRejectsCodeChange() {
        UUID assessmentId = UUID.randomUUID();
        AssessmentEntity assessment = new AssessmentEntity(
                AssessmentCode.PHQ_9,
                "PHQ-9",
                null,
                1
        );
        when(assessmentRepository.findByIdAndDeletedAtIsNull(assessmentId))
                .thenReturn(Optional.of(assessment));

        ResourceConflictException exception = assertThrows(
                ResourceConflictException.class,
                () -> service.updateAssessment(assessmentId, validUpdateRequest(AssessmentCode.GAD_7))
        );

        assertEquals("ASSESSMENT_CODE_IMMUTABLE", exception.getCode());
        verifyNoInteractions(questionRepository);
        verifyNoInteractions(answerOptionRepository);
    }

    private UpsertAssessmentRequest validUpdateRequest(AssessmentCode code) {
        return new UpsertAssessmentRequest(
                code,
                code.value(),
                null,
                List.of(new UpsertQuestionRequest(
                        "Question",
                        0
                ))
        );
    }

    private AssessmentEntity publishedAssessment(UUID assessmentId) {
        AssessmentEntity assessment = new AssessmentEntity(
                AssessmentCode.PHQ_9,
                "PHQ-9",
                null,
                1
        );
        ReflectionTestUtils.setField(assessment, "id", assessmentId);
        assessment.publish();
        return assessment;
    }

    private QuestionEntity question(AssessmentEntity assessment, UUID questionId) {
        QuestionEntity question = new QuestionEntity(assessment, "Question", 0);
        ReflectionTestUtils.setField(question, "id", questionId);
        return question;
    }

    private AnswerOptionEntity option(QuestionEntity question, UUID optionId, int score) {
        AnswerOptionEntity option = new AnswerOptionEntity(question, "Option", score, score);
        ReflectionTestUtils.setField(option, "id", optionId);
        return option;
    }

    private AssessmentResultEntity assessmentResult(
            UUID userId,
            UUID resultId,
            String createdAt,
            int totalScore
    ) {
        AssessmentEntity assessment = publishedAssessment(UUID.randomUUID());
        AssessmentResultEntity result = new AssessmentResultEntity(
                userId,
                assessment,
                totalScore,
                "MILD",
                new ObjectMapper().createArrayNode(),
                1,
                "phq-9-scoring-v1",
                "history-" + resultId,
                "submission-hash",
                "Screening notice",
                new ObjectMapper().createArrayNode()
        );
        ReflectionTestUtils.setField(result, "id", resultId);
        ReflectionTestUtils.setField(result, "createdAt", OffsetDateTime.parse(createdAt));
        return result;
    }

    private AssessmentResultResponse resultResponse(
            UUID resultId,
            int totalScore,
            OffsetDateTime createdAt
    ) {
        return new AssessmentResultResponse(
                resultId,
                AssessmentCode.PHQ_9,
                1,
                totalScore,
                "MILD",
                "Screening notice",
                List.of(),
                createdAt
        );
    }
}
