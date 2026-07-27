package com.mindcare.bookingservice.consultation.service;

import com.mindcare.bookingservice.booking.entity.BookingStatus;
import com.mindcare.bookingservice.booking.service.BookingAccessService;
import com.mindcare.bookingservice.booking.service.BookingAccessSnapshot;
import com.mindcare.bookingservice.consultation.dto.ConsultationNoteResponse;
import com.mindcare.bookingservice.consultation.dto.UpsertConsultationNoteRequest;
import com.mindcare.bookingservice.consultation.dto.UserConsultationNoteResponse;
import com.mindcare.bookingservice.consultation.entity.ConsultationNote;
import com.mindcare.bookingservice.consultation.mapper.ConsultationNoteMapper;
import com.mindcare.bookingservice.consultation.repository.ConsultationNoteRepository;
import com.mindcare.bookingservice.shared.config.BookingPolicyProperties;
import com.mindcare.bookingservice.shared.exception.BusinessException;
import com.mindcare.bookingservice.shared.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsultationNoteServiceImpl implements ConsultationNoteService {

    private final ConsultationNoteRepository noteRepository;
    private final BookingAccessService bookingAccessService;
    private final ConsultationNoteMapper noteMapper;
    private final BookingPolicyProperties policy;
    private final Clock clock;

    @Override
    @Transactional
    public ConsultationNoteResponse upsert(
            UUID expertUserId,
            UUID bookingId,
            UpsertConsultationNoteRequest request) {
        BookingAccessSnapshot access = bookingAccessService.getRequiredForUpdate(bookingId);
        if (!access.expertUserId().equals(expertUserId)) {
            throw new ResourceNotFoundException();
        }
        requireEditable(access);
        ConsultationNote note = noteRepository.findByBookingIdAndDeletedAtIsNull(bookingId)
                .orElseGet(() -> ConsultationNote.create(
                        bookingId,
                        expertUserId,
                        request.observation(),
                        request.recommendation(),
                        request.recoveryPlan(),
                        request.visibleToUser()));
        if (note.getId() != null) {
            note.update(
                    request.observation(),
                    request.recommendation(),
                    request.recoveryPlan(),
                    request.visibleToUser());
        }
        return noteMapper.toExpertResponse(noteRepository.save(note));
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationNoteResponse getForExpert(UUID expertUserId, UUID bookingId) {
        BookingAccessSnapshot access = bookingAccessService.getRequired(bookingId);
        if (!access.expertUserId().equals(expertUserId)) {
            throw new ResourceNotFoundException();
        }
        return noteMapper.toExpertResponse(getNote(bookingId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserConsultationNoteResponse getForUser(UUID userId, UUID bookingId) {
        BookingAccessSnapshot access = bookingAccessService.getRequired(bookingId);
        if (!access.userId().equals(userId)) {
            throw new ResourceNotFoundException();
        }
        ConsultationNote note = getNote(bookingId);
        if (!note.isVisibleToUser()) {
            throw new ResourceNotFoundException();
        }
        return noteMapper.toUserResponse(note);
    }

    private ConsultationNote getNote(UUID bookingId) {
        return noteRepository.findByBookingIdAndDeletedAtIsNull(bookingId)
                .orElseThrow(ResourceNotFoundException::new);
    }

    private void requireEditable(BookingAccessSnapshot access) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        boolean validState = access.status() == BookingStatus.CONFIRMED
                || access.status() == BookingStatus.COMPLETED;
        boolean beforeLock = access.status() != BookingStatus.COMPLETED
                || !now.isAfter(access.endAt().plus(policy.completionReviewTimeout()));
        if (!validState || !beforeLock) {
            throw new BusinessException(
                    "CONSULTATION_NOTE_LOCKED",
                    HttpStatus.CONFLICT,
                    "Consultation note can no longer be edited");
        }
    }
}
