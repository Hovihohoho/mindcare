package com.mindcare.bookingservice.schedule.repository;

import com.mindcare.bookingservice.schedule.entity.ExpertSchedule;
import com.mindcare.bookingservice.schedule.entity.ScheduleStatus;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpertScheduleRepository extends JpaRepository<ExpertSchedule, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT schedule
            FROM ExpertSchedule schedule
            WHERE schedule.id = :id
              AND schedule.deletedAt IS NULL
            """)
    Optional<ExpertSchedule> findActiveByIdForUpdate(@Param("id") UUID id);

    @Query(value = """
            SELECT pg_advisory_xact_lock(
                hashtextextended(CAST(:expertUserId AS text), 0)
            )
            """, nativeQuery = true)
    void acquireExpertScheduleLock(@Param("expertUserId") UUID expertUserId);

    @Query("""
            SELECT CASE WHEN COUNT(schedule) > 0 THEN TRUE ELSE FALSE END
            FROM ExpertSchedule schedule
            WHERE schedule.expertUserId = :expertUserId
              AND schedule.deletedAt IS NULL
              AND schedule.status <> com.mindcare.bookingservice.schedule.entity.ScheduleStatus.CANCELLED
              AND (:excludedId IS NULL OR schedule.id <> :excludedId)
              AND schedule.startAt < :endAt
              AND schedule.endAt > :startAt
            """)
    boolean existsOverlapping(
            @Param("expertUserId") UUID expertUserId,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt,
            @Param("excludedId") UUID excludedId);

    @Query("""
            SELECT schedule
            FROM ExpertSchedule schedule
            WHERE schedule.expertUserId = :expertUserId
              AND (:status IS NULL OR schedule.status = :status)
              AND schedule.deletedAt IS NULL
              AND schedule.startAt >= :from
              AND schedule.startAt < :to
            ORDER BY schedule.startAt ASC, schedule.id ASC
            """)
    List<ExpertSchedule> findByExpertAndRange(
            @Param("expertUserId") UUID expertUserId,
            @Param("status") ScheduleStatus status,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT schedule
            FROM ExpertSchedule schedule
            WHERE schedule.status = com.mindcare.bookingservice.schedule.entity.ScheduleStatus.HELD
              AND schedule.holdExpiresAt <= :now
              AND schedule.deletedAt IS NULL
            ORDER BY schedule.holdExpiresAt ASC
            """)
    List<ExpertSchedule> findExpiredHoldsForUpdate(
            @Param("now") OffsetDateTime now,
            Pageable pageable);

    @Query("""
            SELECT schedule
            FROM ExpertSchedule schedule
            WHERE schedule.status = com.mindcare.bookingservice.schedule.entity.ScheduleStatus.BOOKED
              AND schedule.deletedAt IS NULL
              AND schedule.startAt > :from
              AND schedule.startAt <= :to
            ORDER BY schedule.startAt ASC
            """)
    List<ExpertSchedule> findBookedStartingBetween(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable);
}
