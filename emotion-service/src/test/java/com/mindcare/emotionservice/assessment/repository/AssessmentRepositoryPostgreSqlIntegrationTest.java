package com.mindcare.emotionservice.assessment.repository;

import com.mindcare.emotionservice.assessment.dto.AdminAssessmentResponse;
import com.mindcare.emotionservice.assessment.dto.AssessmentAnswerRequest;
import com.mindcare.emotionservice.assessment.dto.AssessmentResultResponse;
import com.mindcare.emotionservice.assessment.dto.AssessmentSubmissionRequest;
import com.mindcare.emotionservice.assessment.dto.UpsertAssessmentRequest;
import com.mindcare.emotionservice.assessment.dto.UpsertQuestionRequest;
import com.mindcare.emotionservice.assessment.entity.AnswerOptionEntity;
import com.mindcare.emotionservice.assessment.entity.AssessmentEntity;
import com.mindcare.emotionservice.assessment.entity.AssessmentCode;
import com.mindcare.emotionservice.assessment.entity.AssessmentStatus;
import com.mindcare.emotionservice.assessment.entity.QuestionEntity;
import com.mindcare.emotionservice.assessment.service.AssessmentService;
import com.mindcare.emotionservice.shared.exception.ResourceConflictException;
import com.mindcare.emotionservice.shared.exception.ResourceNotFoundException;
import com.mindcare.emotionservice.support.AbstractPostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class AssessmentRepositoryPostgreSqlIntegrationTest extends AbstractPostgreSqlIntegrationTest {

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerOptionRepository answerOptionRepository;

    @Autowired
    private AssessmentResultRepository assessmentResultRepository;

    @Autowired
    private AssessmentService assessmentService;

    @Test
    void persistsAndQueriesAssessmentVersionAndLifecycle() {
        AssessmentEntity draft = assessmentRepository.saveAndFlush(
                new AssessmentEntity(AssessmentCode.PHQ_9, "PHQ-9", "screening", 1)
        );
        draft.publish();
        assessmentRepository.saveAndFlush(draft);

        assertThat(assessmentRepository.findByCodeAndStatusAndDeletedAtIsNull(
                AssessmentCode.PHQ_9,
                AssessmentStatus.PUBLISHED
        )).contains(draft);
        assertThat(assessmentRepository.findByCodeAndAssessmentVersionAndDeletedAtIsNull(
                AssessmentCode.PHQ_9,
                1
        ))
                .contains(draft);
        assertThat(assessmentRepository.findForAdmin(
                false,
                null,
                false,
                null,
                null,
                PageRequest.of(0, 10)
        )).containsExactly(draft);
        assertThat(assessmentRepository.findForAdmin(
                true,
                AssessmentStatus.PUBLISHED,
                false,
                null,
                null,
                PageRequest.of(0, 10)
                )).containsExactly(draft);
    }

    @Test
    void persistsDraftCatalogWithQuestionsAndOrderedAnswerOptions() {
        AssessmentEntity assessment = assessmentRepository.saveAndFlush(
                new AssessmentEntity(
                        AssessmentCode.WHO_5,
                        "WHO-5",
                        "Well-being screening",
                        1
                )
        );
        QuestionEntity question = questionRepository.saveAndFlush(
                new QuestionEntity(assessment, "I have felt cheerful and in good spirits", 0)
        );
        answerOptionRepository.saveAllAndFlush(java.util.List.of(
                new AnswerOptionEntity(question, "At no time", 0, 0),
                new AnswerOptionEntity(question, "All of the time", 5, 1)
        ));

        assertThat(questionRepository
                .findByAssessment_IdAndDeletedAtIsNullOrderByOrderIndexAsc(assessment.getId()))
                .containsExactly(question);
        assertThat(answerOptionRepository
                .findByQuestion_IdAndDeletedAtIsNullOrderByOrderIndexAsc(question.getId()))
                .extracting(
                        AnswerOptionEntity::getOrderIndex,
                        AnswerOptionEntity::getScoreValue
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(0, 0),
                        org.assertj.core.groups.Tuple.tuple(1, 5)
                );
        assertThat(assessmentRepository.findByCodeAndAssessmentVersionAndDeletedAtIsNull(
                AssessmentCode.WHO_5,
                1
        )).contains(assessment);
    }

    @Test
    void updateDraftAtomicallySoftDeletesOldCatalogAndPersistsReplacement() {
        AssessmentEntity assessment = assessmentRepository.saveAndFlush(
                new AssessmentEntity(AssessmentCode.PSS_10, "Old PSS-10", "Old description", 1)
        );
        QuestionEntity oldQuestion = questionRepository.saveAndFlush(
                new QuestionEntity(assessment, "Old question", 0)
        );
        AnswerOptionEntity oldOption = answerOptionRepository.saveAndFlush(
                new AnswerOptionEntity(oldQuestion, "Old option", 0, 0)
        );

        AdminAssessmentResponse response = assessmentService.updateAssessment(
                assessment.getId(),
                new UpsertAssessmentRequest(
                        AssessmentCode.PSS_10,
                        "Updated PSS-10",
                        "Updated description",
                        java.util.List.of(new UpsertQuestionRequest(
                                "Updated question",
                                0
                        ))
                )
        );

        assertThat(response.id()).isEqualTo(assessment.getId());
        assertThat(response.assessmentVersion()).isOne();
        assertThat(response.status()).isEqualTo("DRAFT");
        assertThat(response.title()).isEqualTo("Updated PSS-10");
        assertThat(response.questions()).hasSize(1);
        assertThat(response.questions().get(0).questionText()).isEqualTo("Updated question");
        assertThat(response.questions().get(0).answerOptions())
                .extracting(option -> option.scoreValue())
                .containsExactly(0, 1, 2, 3, 4);

        assertThat(questionRepository.findById(oldQuestion.getId()).orElseThrow().getDeletedAt())
                .isNotNull();
        assertThat(answerOptionRepository.findById(oldOption.getId()).orElseThrow().getDeletedAt())
                .isNotNull();
        assertThat(questionRepository
                .findByAssessment_IdAndDeletedAtIsNullOrderByOrderIndexAsc(assessment.getId()))
                .extracting(QuestionEntity::getQuestionText)
                .containsExactly("Updated question");
    }

    @Test
    void publishesAndArchivesCompleteAssessmentThroughLifecycleService() {
        AdminAssessmentResponse draft = assessmentService.createAssessment(
                new UpsertAssessmentRequest(
                        AssessmentCode.PHQ_9,
                        "PHQ-9",
                        "Screening questionnaire",
                        java.util.stream.IntStream.range(0, 9)
                                .mapToObj(index -> new UpsertQuestionRequest(
                                        "Question " + (index + 1),
                                        index
                                ))
                                .toList()
                )
        );

        AdminAssessmentResponse published = assessmentService.publishAssessment(draft.id());
        assertThat(assessmentService.getPublishedAssessments())
                .extracting(summary -> summary.code())
                .containsExactly(AssessmentCode.PHQ_9);
        assertThat(assessmentService.getPublishedAssessment(AssessmentCode.PHQ_9).questions())
                .hasSize(9)
                .allSatisfy(question -> assertThat(question.answerOptions())
                        .allSatisfy(option -> assertThat(option.optionText()).isNotBlank()));
        AdminAssessmentResponse archived = assessmentService.archiveAssessment(draft.id());
        AdminAssessmentResponse detail = assessmentService.getAssessmentForAdmin(draft.id());

        assertThat(published.status()).isEqualTo("PUBLISHED");
        assertThat(archived.status()).isEqualTo("ARCHIVED");
        assertThat(assessmentService.getPublishedAssessments()).isEmpty();
        assertThatThrownBy(() -> assessmentService.getPublishedAssessment(AssessmentCode.PHQ_9))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(detail.status()).isEqualTo("ARCHIVED");
        assertThat(detail.questions()).hasSize(9);
        assertThat(archived.questions()).hasSize(9);
        assertThat(archived.questions())
                .allSatisfy(question -> assertThat(question.answerOptions())
                        .extracting(option -> option.scoreValue())
                        .containsExactly(0, 1, 2, 3));
        assertThat(assessmentRepository.findByIdAndDeletedAtIsNull(draft.id()))
                .get()
                .extracting(AssessmentEntity::getStatus)
                .isEqualTo(AssessmentStatus.ARCHIVED);
    }

    @Test
    void createsNextDraftVersionWithoutMutatingPublishedCatalog() {
        AdminAssessmentResponse initialDraft = assessmentService.createAssessment(
                new UpsertAssessmentRequest(
                        AssessmentCode.GAD_7,
                        "GAD-7",
                        "Published description",
                        java.util.stream.IntStream.range(0, 7)
                                .mapToObj(index -> new UpsertQuestionRequest(
                                        "Question " + (index + 1),
                                        index
                                ))
                                .toList()
                )
        );
        AdminAssessmentResponse published = assessmentService.publishAssessment(initialDraft.id());

        AdminAssessmentResponse nextDraft =
                assessmentService.createNextAssessmentVersion(published.id());

        assertThat(nextDraft.id()).isNotEqualTo(published.id());
        assertThat(nextDraft.code()).isEqualTo(AssessmentCode.GAD_7);
        assertThat(nextDraft.assessmentVersion()).isEqualTo(2);
        assertThat(nextDraft.status()).isEqualTo("DRAFT");
        assertThat(nextDraft.title()).isEqualTo(published.title());
        assertThat(nextDraft.description()).isEqualTo(published.description());
        assertThat(nextDraft.questions()).hasSize(7);
        assertThat(nextDraft.questions())
                .allSatisfy(question -> assertThat(question.answerOptions())
                        .extracting(option -> option.scoreValue())
                        .containsExactly(0, 1, 2, 3));
        assertThat(assessmentRepository.findByIdAndDeletedAtIsNull(published.id()))
                .get()
                .extracting(AssessmentEntity::getStatus)
                .isEqualTo(AssessmentStatus.PUBLISHED);

        assertThatThrownBy(() -> assessmentService.createNextAssessmentVersion(published.id()))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("draft version already exists");
    }

    @Test
    void submitsPublishedAssessmentAndReplaysIdempotentlyOnPostgreSql() {
        AdminAssessmentResponse draft = assessmentService.createAssessment(
                new UpsertAssessmentRequest(
                        AssessmentCode.PHQ_9,
                        "PHQ-9",
                        "Screening questionnaire",
                        java.util.stream.IntStream.range(0, 9)
                                .mapToObj(index -> new UpsertQuestionRequest(
                                        "Question " + (index + 1),
                                        index
                                ))
                                .toList()
                )
        );
        assessmentService.publishAssessment(draft.id());
        var published = assessmentService.getPublishedAssessment(AssessmentCode.PHQ_9);
        java.util.List<AssessmentAnswerRequest> answers =
                java.util.stream.IntStream.range(0, published.questions().size())
                        .mapToObj(index -> {
                            var question = published.questions().get(index);
                            var option = question.answerOptions().get(index % 4);
                            return new AssessmentAnswerRequest(question.id(), option.id());
                        })
                        .toList();
        AssessmentSubmissionRequest request =
                new AssessmentSubmissionRequest(published.assessmentVersion(), answers);
        java.util.UUID userId = java.util.UUID.randomUUID();

        AssessmentResultResponse created = assessmentService.submitAssessment(
                userId,
                AssessmentCode.PHQ_9,
                "postgres-submission-001",
                request
        );
        AssessmentResultResponse replayed = assessmentService.submitAssessment(
                userId,
                AssessmentCode.PHQ_9,
                "postgres-submission-001",
                request
        );

        assertThat(created.resultId()).isNotNull();
        assertThat(replayed.resultId()).isEqualTo(created.resultId());
        assertThat(created.assessmentCode()).isEqualTo(AssessmentCode.PHQ_9);
        assertThat(created.assessmentVersion()).isOne();
        assertThat(created.totalScore()).isEqualTo(12);
        assertThat(created.riskLevel()).isEqualTo("MODERATE");
        assertThat(created.screeningNotice()).doesNotContainIgnoringCase("chẩn đoán xác định");
        assertThat(assessmentResultRepository.count()).isOne();
        assertThat(assessmentResultRepository.findByIdAndUserIdAndDeletedAtIsNull(
                created.resultId(),
                userId
        )).isPresent();
        assertThat(assessmentService.getAssessmentResult(userId, created.resultId()))
                .isEqualTo(created);
        assertThat(assessmentService.getAssessmentHistory(
                userId,
                created.createdAt().minusDays(1),
                created.createdAt().plusDays(1),
                null,
                20
        ).items()).containsExactly(created);
        assertThatThrownBy(() -> assessmentService.getAssessmentResult(
                java.util.UUID.randomUUID(),
                created.resultId()
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Assessment result was not found");
    }
}
