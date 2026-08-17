package com.mindcare.ai_service.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class KnowledgeVectorRepository {
    private final JdbcTemplate jdbcTemplate;

    public void updateEmbedding(UUID id, List<Double> embedding) {
        jdbcTemplate.update("UPDATE ai_schema.knowledge_documents SET embedding = ?::public.vector, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                toVector(embedding), id);
    }

    public List<SimilarityResult> search(List<Double> queryEmbedding, int limit, double threshold) {
        String sql = """
                SELECT id, title, content, source_url,
                       1 - (embedding OPERATOR(public.<=>) ?::public.vector) AS similarity
                FROM ai_schema.knowledge_documents
                WHERE is_active = TRUE
                  AND review_status = 'APPROVED'
                  AND source_tier IN ('A', 'B')
                  AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)
                  AND source_url LIKE 'https://%'
                  AND embedding IS NOT NULL
                  AND 1 - (embedding OPERATOR(public.<=>) ?::public.vector) >= ?
                ORDER BY embedding OPERATOR(public.<=>) ?::public.vector
                LIMIT ?
                """;
        String vector = toVector(queryEmbedding);
        return jdbcTemplate.query(sql, (rs, rowNum) -> new SimilarityResult(
                rs.getObject("id", UUID.class), rs.getString("title"), rs.getString("content"),
                rs.getString("source_url"), rs.getDouble("similarity")),
                vector, vector, threshold, vector, limit);
    }

    private String toVector(List<Double> values) {
        return values.stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
    }

    public record SimilarityResult(UUID id, String title, String content, String sourceUrl, double similarity) {}
}
