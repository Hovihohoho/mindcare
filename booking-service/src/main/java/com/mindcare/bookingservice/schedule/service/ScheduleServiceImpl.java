package com.mindcare.bookingservice.schedule.service;

import com.mindcare.bookingservice.integration.auth.ExpertBookingProfile;
import com.mindcare.bookingservice.integration.auth.ExpertProfileGateway;
import com.mindcare.bookingservice.schedule.dto.CreateScheduleRequest;
import com.mindcare.bookingservice.schedule.dto.ScheduleResponse;
import com.mindcare.bookingservice.schedule.dto.UpdateScheduleRequest;
import com.mindcare.bookingservice.schedule.entity.ExpertSchedule;
import com.mindcare.bookingservice.schedule.entity.ScheduleStatus;
import com.mindcare.bookingservice.schedule.mapper.ScheduleMapper;
import com.mindcare.bookingservice.schedule.repository.ExpertScheduleRepository;
import com.mindcare.bookingservice.shared.exception.BusinessException;
import com.mindcare.bookingservice.shared.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService, ScheduleLifecycleService {

    private final ExpertScheduleRepository scheduleRepository;
    private final ExpertProfileGateway expertProfileGateway;
    private final ScheduleMapper scheduleMapper;
    private final Clock clock;

    @Override
    @Transactional
    public ScheduleResponse create(UUID expertUserId, CreateScheduleRequest request) {
        requireEligibleExpert(expertUserId);
        validateFutureRange(request.startAt(), request.endAt());
        scheduleRepository.acquireExpertScheduleLock(expertUserId);
        rejectOverlap(expertUserId, request.startAt(), request.endAt(), null);
        ExpertSchedule schedule = ExpertSchedule.create(
                expertUserId,
                request.startAt(),
                request.endAt());
        return scheduleMapper.toResponse(scheduleRepository.save(schedule));
    }

    @Override
    @Transactional
    public ScheduleResponse update(
            UUID expertUserId,
            UUID scheduleId,
            UpdateScheduleRequest request) {
        requireEligibleExpert(expertUserId);
        validateFutureRange(request.startAt(), request.endAt());
        scheduleRepository.acquireExpertScheduleLock(expertUserId);
        ExpertSchedule schedule = getOwnedForUpdate(expertUserId, scheduleId);
        rejectOverlap(
                expertUserId,
                request.startAt(),
                request.endAt(),
                scheduleId);
        schedule.updateTime(request.startAt(), request.endAt());
        return scheduleMapper.toResponse(schedule);
    }

    @Override
    @Transactional
    public void cancel(UUID expertUserId, UUID scheduleId) {
        ExpertSchedule schedule = getOwnedForUpdate(expertUserId, scheduleId);
        schedule.cancel();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleResponse> listOwn(
            UUID expertUserId,
            OffsetDateTime from,
            OffsetDateTime to,
            int limit) {
        validateQueryRange(from, to);
        return scheduleRepository.findByExpertAndRange(
                        expertUserId,
                        null,
                        from,
                        to,
                        PageRequest.of(0, normalizeLimit(limit)))
                .stream()
                .map(scheduleMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleResponse> listAvailable(
            UUID expertUserId,
            OffsetDateTime from,
            OffsetDateTime to,
            int limit) {
        validateQueryRange(from, to);
        return scheduleRepository.findByExpertAndRange(
                        expertUserId,
                        ScheduleStatus.AVAILABLE,
                        from,
                        to,
                        PageRequest.of(0, normalizeLimit(limit)))
                .stream()
                .map(scheduleMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ScheduleSnapshot hold(UUID scheduleId, OffsetDateTime expiresAt) {
        ExpertSchedule schedule = scheduleRepository.findActiveByIdForUpdate(scheduleId)
                .orElseThrow(ResourceNotFoundException::new);
        if (schedule.getStatus() != ScheduleStatus.AVAILABLE) {
            throw new BusinessException(
                    "SCHEDULE_NOT_AVAILABLE",
                    HttpStatus.CONFLICT,
                    "The selected schedule is no longer available");
        }
        schedule.holdUntil(expiresAt);
        return snapshot(schedule);
    }

    @Override
    @Transactional
    public void confirm(UUID scheduleId) {
        scheduleRepository.findActiveByIdForUpdate(scheduleId)
                .orElseThrow(ResourceNotFoundException::new)
                .confirmBooked();
    }

    @Override
    @Transactional
    public void releaseHold(UUID scheduleId) {
        scheduleRepository.findActiveByIdForUpdate(scheduleId)
                .orElseThrow(ResourceNotFoundException::new)
                .releaseHold();
    }

    @Override
    @Transactional
    public void releaseBooking(UUID scheduleId) {
        scheduleRepository.findActiveByIdForUpdate(scheduleId)
                .orElseThrow(ResourceNotFoundException::new)
                .releaseBooking();
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleSnapshot get(UUID scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .filter(schedule -> !schedule.isDeleted())
                .map(this::snapshot)
                .orElseThrow(ResourceNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleSnapshot> findBookedStartingBetween(
            OffsetDateTime from, OffsetDateTime to, int limit) {
        return scheduleRepository.findBookedStartingBetween(from, to,
                        PageRequest.of(0, normalizeLimit(limit))).stream()
                .map(this::snapshot)
                .toList();
    }

    private ExpertSchedule getOwnedForUpdate(UUID expertUserId, UUID scheduleId) {
        ExpertSchedule schedule = scheduleRepository.findActiveByIdForUpdate(scheduleId)
                .orElseThrow(ResourceNotFoundException::new);
        if (!schedule.getExpertUserId().equals(expertUserId)) {
            throw new ResourceNotFoundException();
        }
        return schedule;
    }

    private void requireEligibleExpert(UUID expertUserId) {
        ExpertBookingProfile profile = expertProfileGateway.getBookingProfile(expertUserId);
        if (!profile.expertUserId().equals(expertUserId) || !profile.eligible()) {
            throw new BusinessException(
                    "EXPERT_NOT_ELIGIBLE",
                    HttpStatus.FORBIDDEN,
                    "Expert is not eligible for booking operations");
        }
    }

    private void validateFutureRange(OffsetDateTime startAt, OffsetDateTime endAt) {
        if (!startAt.isAfter(OffsetDateTime.now(clock)) || !startAt.isBefore(endAt)) {
            throw new BusinessException(
                    "INVALID_SCHEDULE_RANGE",
                    HttpStatus.BAD_REQUEST,
                    "Schedule must be a valid future range");
        }
    }

    private void validateQueryRange(OffsetDateTime from, OffsetDateTime to) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new BusinessException(
                    "INVALID_TIME_RANGE",
                    HttpStatus.BAD_REQUEST,
                    "Time range must use [from,to) with from before to");
        }
    }

    private void rejectOverlap(
            UUID expertUserId,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            UUID excludedId) {
        if (scheduleRepository.existsOverlapping(
                expertUserId,
                startAt,
                endAt,
                excludedId)) {
            throw new BusinessException(
                    "SCHEDULE_OVERLAP",
                    HttpStatus.CONFLICT,
                    "Schedule overlaps another active slot");
        }
    }

    private ScheduleSnapshot snapshot(ExpertSchedule schedule) {
        return new ScheduleSnapshot(
                schedule.getId(),
                schedule.getExpertUserId(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                schedule.getStatus(),
                schedule.getHoldExpiresAt());
    }

    private int normalizeLimit(int limit) {
        return Math.max(1, Math.min(limit, 100));
    }
}
