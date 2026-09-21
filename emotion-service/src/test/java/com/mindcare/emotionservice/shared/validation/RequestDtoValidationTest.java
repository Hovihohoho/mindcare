package com.mindcare.emotionservice.shared.validation;

import com.mindcare.emotionservice.assessment.dto.AssessmentAnswerRequest;
import com.mindcare.emotionservice.assessment.dto.AssessmentSubmissionRequest;
import com.mindcare.emotionservice.assessment.dto.UpsertAssessmentRequest;
import com.mindcare.emotionservice.assessment.dto.UpsertQuestionRequest;
import com.mindcare.emotionservice.assessment.entity.AssessmentCode;
import com.mindcare.emotionservice.healthmetric.dto.HealthMetricBatchRequest;
import com.mindcare.emotionservice.healthmetric.dto.HealthMetricItemRequest;
import com.mindcare.emotionservice.journal.dto.CreateEmotionJournalRequest;
import com.mindcare.emotionservice.journal.entity.EmotionType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class RequestDtoValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void journalContentUsesUnicodeCodePointLimit() {
        String exactlyFiveThousandEmoji = "😀".repeat(5_000);
        String tooManyEmoji = exactlyFiveThousandEmoji + "😀";

        assertThat(validator.validate(new CreateEmotionJournalRequest(
                EmotionType.HAPPY,
                exactlyFiveThousandEmoji
        ))).isEmpty();
        assertThat(paths(validator.validate(new CreateEmotionJournalRequest(
                EmotionType.HAPPY,
                tooManyEmoji
        )))).containsExactly("content");
    }

    @Test
    void dailyCheckInSignalsMustStayWithinOneToFive() {
        var valid = new CreateEmotionJournalRequest(EmotionType.NEUTRAL, null, 1, 5, 3);
        var invalid = new CreateEmotionJournalRequest(EmotionType.NEUTRAL, null, 0, 6, -1);

        assertThat(validator.validate(valid)).isEmpty();
        assertThat(paths(validator.validate(invalid))).containsExactlyInAnyOrder(
                "energyLevel", "stressLevel", "sleepQuality");
    }

    @Test
    void healthBatchValidatesSizeAndNestedItems() {
        HealthMetricBatchRequest invalid = new HealthMetricBatchRequest(
                " ",
                List.of(new HealthMetricItemRequest(
                        "x".repeat(256),
                        "",
                        null,
                        "",
                        null
                ))
        );

        assertThat(paths(validator.validate(invalid))).contains(
                "sourceType",
                "items[0].externalSampleId",
                "items[0].metricType"
        );

        List<HealthMetricItemRequest> tooManyItems = IntStream.rangeClosed(0, 100)
                .mapToObj(index -> validMetric("sample-" + index))
                .toList();
        assertThat(paths(validator.validate(new HealthMetricBatchRequest(
                "APPLE_HEALTH",
                tooManyItems
        )))).contains("items");
    }

    @Test
    void assessmentSubmissionValidatesVersionAndNestedIdentifiers() {
        AssessmentSubmissionRequest request = new AssessmentSubmissionRequest(
                0,
                List.of(new AssessmentAnswerRequest(null, null))
        );

        assertThat(paths(validator.validate(request))).contains(
                "assessmentVersion",
                "answers[0].questionId",
                "answers[0].optionId"
        );
    }

    @Test
    void assessmentCatalogValidatesNestedQuestionShape() {
        UpsertAssessmentRequest request = new UpsertAssessmentRequest(
                null,
                "",
                null,
                List.of(new UpsertQuestionRequest(
                        "",
                        -1
                ))
        );

        assertThat(paths(validator.validate(request))).contains(
                "code",
                "title",
                "questions[0].questionText",
                "questions[0].orderIndex"
        );
    }

    @Test
    void assessmentCatalogRejectsManualAnswerOptions() {
        UpsertAssessmentRequest request = new UpsertAssessmentRequest(
                AssessmentCode.PHQ_9,
                "PHQ-9",
                null,
                List.of(new UpsertQuestionRequest(
                        "Question",
                        0,
                        List.of("manual-option")
                ))
        );

        assertThat(paths(validator.validate(request)))
                .containsExactly("questions[0].answerOptions");
    }

    @Test
    void nullableCollectionsReachValidationInsteadOfThrowingFromConstructors() {
        assertThat(paths(validator.validate(new HealthMetricBatchRequest(
                "MANUAL",
                null
        )))).contains("items");
        assertThat(paths(validator.validate(new AssessmentSubmissionRequest(
                1,
                null
        )))).contains("answers");
        assertThat(paths(validator.validate(new UpsertAssessmentRequest(
                AssessmentCode.PHQ_9,
                "PHQ-9",
                null,
                null
        )))).contains("questions");
    }

    private static HealthMetricItemRequest validMetric(String externalSampleId) {
        return new HealthMetricItemRequest(
                externalSampleId,
                "HEART_RATE",
                BigDecimal.valueOf(70),
                "bpm",
                OffsetDateTime.now(ZoneOffset.UTC)
        );
    }

    private static Set<String> paths(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
