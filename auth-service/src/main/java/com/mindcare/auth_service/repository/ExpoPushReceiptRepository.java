package com.mindcare.auth_service.repository;

import com.mindcare.auth_service.entity.ExpoPushReceipt;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpoPushReceiptRepository extends JpaRepository<ExpoPushReceipt, UUID> {
    List<ExpoPushReceipt> findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
            String status, OffsetDateTime createdBefore);

    long deleteByStatusNotAndCreatedAtBefore(String status, OffsetDateTime createdBefore);
}
