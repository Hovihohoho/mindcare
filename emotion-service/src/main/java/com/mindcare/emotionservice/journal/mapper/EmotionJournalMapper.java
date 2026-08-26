package com.mindcare.emotionservice.journal.mapper;

import com.mindcare.emotionservice.journal.dto.EmotionJournalResponse;
import com.mindcare.emotionservice.journal.entity.EmotionJournalEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface EmotionJournalMapper {

    EmotionJournalResponse toResponse(EmotionJournalEntity entity);
}
