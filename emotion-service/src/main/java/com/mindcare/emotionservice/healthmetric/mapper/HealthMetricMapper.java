package com.mindcare.emotionservice.healthmetric.mapper;

import com.mindcare.emotionservice.healthmetric.dto.HealthMetricItemRequest;
import com.mindcare.emotionservice.healthmetric.dto.HealthMetricResponse;
import com.mindcare.emotionservice.healthmetric.entity.HealthMetricEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface HealthMetricMapper {

    @Mapping(target = "value", source = "metricValue")
    HealthMetricResponse toResponse(HealthMetricEntity entity);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "metricType", source = "request.metricType")
    @Mapping(target = "metricValue", source = "request.value")
    @Mapping(target = "unit", source = "request.unit")
    @Mapping(target = "sourceType", source = "sourceType")
    @Mapping(target = "externalSampleId", source = "request.externalSampleId")
    @Mapping(target = "recordedAt", source = "request.recordedAt")
    HealthMetricEntity toEntity(
            HealthMetricItemRequest request,
            String sourceType,
            UUID userId
    );
}
