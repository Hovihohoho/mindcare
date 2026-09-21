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

    public void replaceChunks(UUID documentId, String title, List<ChunkEmbedding> chunks) {
        jdbcTemplate.update("DELETE FROM ai_schema.knowledge_chunks WHERE document_id = ?", documentId);
        for (ChunkEmbedding chunk : chunks) {
            jdbcTemplate.update("""
                    INSERT INTO ai_schema.knowledge_chunks
                        (id, document_id, chunk_index, content, token_estimate, embedding)
                    VALUES (?, ?, ?, ?, ?, ?::public.vector)
                    """, UUID.randomUUID(), documentId, chunk.index(), chunk.content(),
                    chunk.tokenEstimate(), toVector(chunk.embedding()));
        }
    }

    public List<SimilarityResult> search(String query, List<Double> queryEmbedding, int limit,
                                         double threshold, boolean includeSafety) {
        String sql = """
                WITH eligible AS (
                    SELECT c.id AS chunk_id, c.document_id, c.chunk_index, c.content, c.embedding,
                           c.search_vector, d.title, d.source_url
                    FROM ai_schema.knowledge_chunks c
                    JOIN ai_schema.knowledge_documents d ON d.id = c.document_id
                    WHERE d.is_active = TRUE AND d.review_status = 'APPROVED'
                      AND d.source_tier IN ('A', 'B')
                      AND (? OR d.document_type <> 'SAFETY')
                      AND (d.expires_at IS NULL OR d.expires_at > CURRENT_TIMESTAMP)
                      AND d.source_url LIKE 'https://%' AND c.embedding IS NOT NULL
                ), semantic AS (
                    SELECT *, 1 - (embedding OPERATOR(public.<=>) ?::public.vector) AS similarity,
                           row_number() OVER (ORDER BY embedding OPERATOR(public.<=>) ?::public.vector) AS rank
                    FROM eligible
                    WHERE 1 - (embedding OPERATOR(public.<=>) ?::public.vector) >= ?
                    ORDER BY embedding OPERATOR(public.<=>) ?::public.vector LIMIT ?
                ), lexical AS (
                    SELECT *, ts_rank_cd(search_vector, plainto_tsquery('simple', ?)) AS lexical_score,
                           row_number() OVER (ORDER BY ts_rank_cd(search_vector, plainto_tsquery('simple', ?)) DESC) AS rank
                    FROM eligible WHERE search_vector @@ plainto_tsquery('simple', ?)
                    ORDER BY lexical_score DESC LIMIT ?
                ), fused AS (
                    SELECT coalesce(s.chunk_id, l.chunk_id) AS chunk_id,
                           coalesce(s.document_id, l.document_id) AS document_id,
                           coalesce(s.chunk_index, l.chunk_index) AS chunk_index,
                           coalesce(s.title, l.title) AS title, coalesce(s.content, l.content) AS content,
                           coalesce(s.source_url, l.source_url) AS source_url,
                           coalesce(s.similarity, 0) AS similarity,
                           coalesce(1.0 / (60 + s.rank), 0) + coalesce(1.0 / (60 + l.rank), 0) AS score
                    FROM semantic s FULL OUTER JOIN lexical l ON s.chunk_id = l.chunk_id
                )
                SELECT * FROM fused ORDER BY score DESC, similarity DESC LIMIT ?
                """;
        String vector = toVector(queryEmbedding);
        return jdbcTemplate.query(sql, (rs, rowNum) -> new SimilarityResult(
                rs.getObject("document_id", UUID.class), rs.getObject("chunk_id", UUID.class),
                rs.getInt("chunk_index"), rs.getString("title"), rs.getString("content"),
                rs.getString("source_url"), rs.getDouble("similarity")),
                includeSafety, vector, vector, vector, threshold, vector, limit * 4,
                query, query, query, limit * 4, limit);
    }

    private String toVector(List<Double> values) {
        return values.stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
    }

    public record ChunkEmbedding(int index, String content, int tokenEstimate, List<Double> embedding) {}
    public record SimilarityResult(UUID id, UUID chunkId, int chunkIndex, String title,
                                   String content, String sourceUrl, double similarity) {}
}
