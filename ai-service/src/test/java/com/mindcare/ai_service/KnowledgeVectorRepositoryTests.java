package com.mindcare.ai_service;

import com.mindcare.ai_service.repository.KnowledgeVectorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class KnowledgeVectorRepositoryTests {
    @Autowired KnowledgeVectorRepository vectorRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void stores768DimensionsAndReturnsClosestDocument() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                        INSERT INTO ai_schema.knowledge_documents
                            (id,title,content,source_url,source_tier,review_status,is_active)
                        VALUES (?,?,?,?,?,?,TRUE)
                        """,
                id, "Vector repository test", "Test content", "https://www.who.int/",
                "A", "APPROVED");
        List<Double> vector = new ArrayList<>(Collections.nCopies(768, 0.0));
        vector.set(0, 1.0);
        vectorRepository.updateEmbedding(id, vector);

        var results = vectorRepository.search(vector, 3, 0.5);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).id()).isEqualTo(id);
        assertThat(results.get(0).similarity()).isGreaterThan(0.99);
    }
}
