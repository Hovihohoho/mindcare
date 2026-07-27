package com.mindcare.emotionservice.journal.mapper;

import com.mindcare.emotionservice.journal.dto.CreateEmotionJournalRequest;
import com.mindcare.emotionservice.journal.dto.EmotionJournalResponse;
import com.mindcare.emotionservice.journal.entity.EmotionJournalEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface EmotionJournalMapper {

    EmotionJournalResponse toResponse(EmotionJournalEntity entity);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "emotionType", source = "request.emotionType")
    @Mapping(target = "content", source = "request.content")
    EmotionJournalEntity toEntity(CreateEmotionJournalRequest request, UUID userId);
}
