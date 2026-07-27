package com.mindcare.emotionservice.risk.mapper;

import com.mindcare.emotionservice.risk.dto.RiskAlertResponse;
import com.mindcare.emotionservice.risk.entity.PsychologicalAlertLogEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RiskAlertMapper {

    RiskAlertResponse toResponse(PsychologicalAlertLogEntity entity);
}
