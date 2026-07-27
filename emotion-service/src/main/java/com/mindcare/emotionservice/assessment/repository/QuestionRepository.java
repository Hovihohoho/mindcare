package com.mindcare.emotionservice.assessment.repository;

import com.mindcare.emotionservice.assessment.entity.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<QuestionEntity, UUID> {

    List<QuestionEntity> findByAssessment_IdAndDeletedAtIsNullOrderByOrderIndexAsc(UUID assessmentId);
}
