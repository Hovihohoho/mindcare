package com.mindcare.bookingservice.consultation.service;

import com.mindcare.bookingservice.consultation.dto.ConsultationNoteResponse;
import com.mindcare.bookingservice.consultation.dto.UpsertConsultationNoteRequest;
import com.mindcare.bookingservice.consultation.dto.UserConsultationNoteResponse;
import java.util.UUID;

public interface ConsultationNoteService {

    ConsultationNoteResponse upsert(
            UUID expertUserId,
            UUID bookingId,
            UpsertConsultationNoteRequest request);

    ConsultationNoteResponse getForExpert(UUID expertUserId, UUID bookingId);

    UserConsultationNoteResponse getForUser(UUID userId, UUID bookingId);
}
