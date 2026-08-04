package com.mindcare.bookingservice.client.service;

import com.mindcare.bookingservice.booking.entity.Booking;
import com.mindcare.bookingservice.booking.repository.BookingRepository;
import com.mindcare.bookingservice.client.dto.ExpertClientProfileResponse;
import com.mindcare.bookingservice.integration.auth.ClientProfile;
import com.mindcare.bookingservice.integration.auth.ClientProfileGateway;
import com.mindcare.bookingservice.shared.exception.ResourceNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpertClientServiceImpl implements ExpertClientService {

    private final BookingRepository bookingRepository;
    private final ClientProfileGateway clientProfileGateway;

    @Override
    @Transactional(readOnly = true)
    public ExpertClientProfileResponse getBySchedule(UUID expertUserId, UUID scheduleId) {
        Booking booking = bookingRepository
                .findByScheduleIdAndExpertUserIdAndDeletedAtIsNull(scheduleId, expertUserId)
                .orElseThrow(ResourceNotFoundException::new);
        return toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpertClientProfileResponse getByUser(UUID expertUserId, UUID userId) {
        Booking booking = bookingRepository
                .findFirstByExpertUserIdAndUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                        expertUserId,
                        userId)
                .orElseThrow(ResourceNotFoundException::new);
        return toResponse(booking);
    }

    private ExpertClientProfileResponse toResponse(Booking booking) {
        ClientProfile profile = clientProfileGateway.getClientProfile(booking.getUserId());
        return new ExpertClientProfileResponse(
                profile.userId(),
                profile.fullName(),
                profile.email(),
                profile.createdAt(),
                booking.getId(),
                booking.getScheduleId(),
                booking.getStatus());
    }
}
