package com.mindcare.bookingservice.chat.repository;

import com.mindcare.bookingservice.chat.entity.Conversation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByBookingIdAndDeletedAtIsNull(UUID bookingId);

    Optional<Conversation> findByIdAndDeletedAtIsNull(UUID id);
}
