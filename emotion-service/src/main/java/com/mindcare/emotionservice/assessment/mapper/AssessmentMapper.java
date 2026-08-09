package com.mindcare.emotionservice.assessment.mapper;

import com.mindcare.emotionservice.assessment.dto.AdminAnswerOptionResponse;
import com.mindcare.emotionservice.assessment.dto.AdminAssessmentResponse;
import com.mindcare.emotionservice.assessment.dto.AdminQuestionResponse;
import com.mindcare.emotionservice.assessment.dto.AnswerOptionResponse;
import com.mindcare.emotionservice.assessment.dto.AssessmentDetailResponse;
import com.mindcare.emotionservice.assessment.dto.AssessmentResultResponse;
import com.mindcare.emotionservice.assessment.dto.AssessmentSummaryResponse;
import com.mindcare.emotionservice.assessment.dto.QuestionResponse;
import com.mindcare.emotionservice.assessment.dto.UpsertAssessmentRequest;
import com.mindcare.emotionservice.assessment.dto.UpsertQuestionRequest;
import com.mindcare.emotionservice.assessment.entity.AnswerOptionEntity;
import com.mindcare.emotionservice.assessment.entity.AssessmentEntity;
import com.mindcare.emotionservice.assessment.entity.AssessmentResultEntity;
import com.mindcare.emotionservice.assessment.entity.QuestionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AssessmentMapper {

    QuestionResponse toQuestionResponse(
            QuestionEntity entity,
            List<AnswerOptionResponse> answerOptions
    );

    AnswerOptionResponse toAnswerOptionResponse(AnswerOptionEntity entity);

    AdminAssessmentResponse toAdminResponse(
            AssessmentEntity entity,
            List<AdminQuestionResponse> questions
    );

    AdminQuestionResponse toAdminQuestionResponse(
            QuestionEntity entity,
            List<AdminAnswerOptionResponse> answerOptions
    );

    AdminAnswerOptionResponse toAdminAnswerOptionResponse(AnswerOptionEntity entity);

    @Mapping(target = "resultId", source = "entity.id")
    @Mapping(target = "assessmentCode", source = "entity.assessment.code")
    @Mapping(target = "recommendations", source = "recommendationTexts")
    AssessmentResultResponse toResultResponse(
            AssessmentResultEntity entity,
            List<String> recommendationTexts
    );

    default AssessmentEntity toEntity(UpsertAssessmentRequest request) {
        return new AssessmentEntity(request.code(), request.title(), request.description());
    }

    @Mapping(target = "assessment", source = "assessment")
    @Mapping(target = "questionText", source = "request.questionText")
    @Mapping(target = "orderIndex", source = "request.orderIndex")
    QuestionEntity toEntity(
            UpsertQuestionRequest request,
            AssessmentEntity assessment
    );

}
