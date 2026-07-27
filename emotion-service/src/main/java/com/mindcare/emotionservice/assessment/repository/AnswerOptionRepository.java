package com.mindcare.emotionservice.assessment.repository;

import com.mindcare.emotionservice.assessment.entity.AnswerOptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnswerOptionRepository extends JpaRepository<AnswerOptionEntity, UUID> {

    List<AnswerOptionEntity> findByQuestion_IdAndDeletedAtIsNullOrderByOrderIndexAsc(UUID questionId);
}
