package com.mindcare.emotionservice.journal.repository;

import com.mindcare.emotionservice.journal.entity.EmotionJournalEntity;
import com.mindcare.emotionservice.journal.entity.EmotionType;
import com.mindcare.emotionservice.support.AbstractPostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class EmotionJournalRepositoryPostgreSqlIntegrationTest extends AbstractPostgreSqlIntegrationTest {

    @Autowired
    private EmotionJournalRepository journalRepository;

    @Test
    void queriesEnforceOwnershipSoftDeleteAndKeysetOrdering() {
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        EmotionJournalEntity first = journalRepository.saveAndFlush(
                new EmotionJournalEntity(ownerId, EmotionType.HAPPY, "first")
        );
        EmotionJournalEntity deleted = journalRepository.saveAndFlush(
                new EmotionJournalEntity(ownerId, EmotionType.SAD, "deleted")
        );
        journalRepository.saveAndFlush(
                new EmotionJournalEntity(otherUserId, EmotionType.NEUTRAL, "other user")
        );
        EmotionJournalEntity second = journalRepository.saveAndFlush(
                new EmotionJournalEntity(ownerId, EmotionType.VERY_HAPPY, "second")
        );
        deleted.softDelete(OffsetDateTime.now(ZoneOffset.UTC));
        journalRepository.saveAndFlush(deleted);

        assertThat(journalRepository.findByIdAndUserIdAndDeletedAtIsNull(first.getId(), ownerId))
                .contains(first);
        assertThat(journalRepository.findByIdAndUserIdAndDeletedAtIsNull(first.getId(), otherUserId))
                .isEmpty();
        assertThat(journalRepository.findByIdAndUserIdAndDeletedAtIsNull(deleted.getId(), ownerId))
                .isEmpty();

        List<EmotionJournalEntity> firstPage = journalRepository.findHistory(
                ownerId,
                first.getCreatedAt().minusMinutes(1),
                OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1),
                false,
                null,
                null,
                PageRequest.of(0, 1)
        );
        assertThat(firstPage).hasSize(1);

        EmotionJournalEntity cursor = firstPage.get(0);
        List<EmotionJournalEntity> secondPage = journalRepository.findHistory(
                ownerId,
                first.getCreatedAt().minusMinutes(1),
                OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1),
                true,
                cursor.getCreatedAt(),
                cursor.getId(),
                PageRequest.of(0, 1)
        );

        assertThat(secondPage).hasSize(1);
        assertThat(List.of(cursor.getId(), secondPage.get(0).getId()))
                .containsExactlyInAnyOrder(first.getId(), second.getId());
    }
}
