package com.mindcare.emotionservice.healthmetric.mapper;

import com.mindcare.emotionservice.healthmetric.dto.HealthMetricResponse;
import com.mindcare.emotionservice.healthmetric.entity.HealthMetricEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface HealthMetricMapper {

    @Mapping(target = "value", source = "metricValue")
    HealthMetricResponse toResponse(HealthMetricEntity entity);
}
