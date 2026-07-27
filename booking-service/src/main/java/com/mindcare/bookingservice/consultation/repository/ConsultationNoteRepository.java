package com.mindcare.bookingservice.consultation.repository;

import com.mindcare.bookingservice.consultation.entity.ConsultationNote;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationNoteRepository extends JpaRepository<ConsultationNote, UUID> {

    Optional<ConsultationNote> findByBookingIdAndDeletedAtIsNull(UUID bookingId);
}
