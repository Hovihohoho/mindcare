package com.mindcare.bookingservice.consultation.mapper;

import com.mindcare.bookingservice.consultation.dto.ConsultationNoteResponse;
import com.mindcare.bookingservice.consultation.dto.UserConsultationNoteResponse;
import com.mindcare.bookingservice.consultation.entity.ConsultationNote;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ConsultationNoteMapper {

    ConsultationNoteResponse toExpertResponse(ConsultationNote note);

    UserConsultationNoteResponse toUserResponse(ConsultationNote note);
}
