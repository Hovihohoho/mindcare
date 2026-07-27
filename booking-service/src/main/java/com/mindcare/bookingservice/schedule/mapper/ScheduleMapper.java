package com.mindcare.bookingservice.schedule.mapper;

import com.mindcare.bookingservice.schedule.dto.ScheduleResponse;
import com.mindcare.bookingservice.schedule.entity.ExpertSchedule;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ScheduleMapper {

    ScheduleResponse toResponse(ExpertSchedule schedule);
}
