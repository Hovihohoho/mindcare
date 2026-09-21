package com.mindcare.ai_service.service;

import com.mindcare.ai_service.config.GeminiProperties;
import com.mindcare.ai_service.dto.RagChatRequest;
import com.mindcare.ai_service.repository.KnowledgeDocumentRepository;
import com.mindcare.ai_service.repository.KnowledgeVectorRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;
import static org.mockito.Mockito.*;

/** Opt-in paid provider evaluation. Reads only the knowledge corpus; never persists conversations. */
@EnabledIfEnvironmentVariable(named = "LIVE_RAG_EVAL", matches = "true")
class RagEvaluationTest {
    @Test
    void evaluateFrozenQuestionsAgainstConfiguredCorpus() throws Exception {
        var mapper = new ObjectMapper();
        Path goldenPath = Path.of("evaluation/golden.vi.v1.json");
        byte[] goldenBytes = Files.readAllBytes(goldenPath);
        var golden = mapper.readTree(goldenBytes);
        var properties = new GeminiProperties(required("GEMINI_API_KEY"),
                env("GEMINI_BASE_URL", "https://generativelanguage.googleapis.com/v1beta"),
                env("GEMINI_EMBEDDING_MODEL", "gemini-embedding-2"),
                env("GEMINI_CHAT_MODEL", "gemini-3.1-flash-lite"),
                env("GEMINI_FALLBACK_CHAT_MODEL", "gemini-3.1-flash-lite"), 768);
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(required("EVAL_DB_URL"),
                required("EVAL_DB_USERNAME"), required("EVAL_DB_PASSWORD")));
        // Fail before any paid call if the database or evaluation corpus is unavailable.
        String corpusFingerprint = jdbc.queryForObject("""
                SELECT md5(coalesce(string_agg(
                    d.id::text || ':' || md5(d.content) || ':' || coalesce(d.source_url, '') || ':' ||
                    coalesce(d.review_status, '') || ':' || coalesce(d.source_tier, '') || ':' ||
                    d.is_active::text || ':' || coalesce(d.expires_at::text, '') || ':' ||
                    c.id::text || ':' || md5(c.content) || ':' || md5(coalesce(c.embedding::text, '')),
                    '|' ORDER BY d.id, c.id), ''))
                FROM ai_schema.knowledge_documents d JOIN ai_schema.knowledge_chunks c ON c.document_id=d.id
                """, String.class);
        Integer eligible = jdbc.queryForObject("""
                SELECT count(*) FROM ai_schema.knowledge_chunks c
                JOIN ai_schema.knowledge_documents d ON d.id=c.document_id
                WHERE d.is_active AND d.review_status='APPROVED' AND d.source_tier IN ('A','B')
                AND (d.expires_at IS NULL OR d.expires_at>CURRENT_TIMESTAMP) AND c.embedding IS NOT NULL
                """, Integer.class);
        if (eligible == null || eligible == 0) throw new IllegalStateException("No eligible indexed corpus");
        var gemini = spy(new GeminiClient(properties, mapper));
        var providerErrors = new AtomicInteger();
        doAnswer(call -> {
            try { return call.callRealMethod(); }
            catch (RuntimeException error) { providerErrors.incrementAndGet(); throw error; }
        }).when(gemini).embed(anyString(), anyString());
        doAnswer(call -> {
            try { return call.callRealMethod(); }
            catch (RuntimeException error) { providerErrors.incrementAndGet(); throw error; }
        }).when(gemini).generate(anyString(), anyString());
        var embeddings = spy(new EmbeddingService(gemini, new KnowledgeVectorRepository(jdbc),
                mock(KnowledgeDocumentRepository.class), new DocumentChunker(1800, 250)));
        var retrieved = new AtomicReference<List<KnowledgeVectorRepository.SimilarityResult>>(List.of());
        doAnswer(call -> {
            @SuppressWarnings("unchecked")
            var result = (List<KnowledgeVectorRepository.SimilarityResult>) call.callRealMethod();
            retrieved.set(result);
            return result;
        }).when(embeddings).search(anyString(), anyInt(), anyDouble(), anyBoolean());
        var chat = new RagChatService(embeddings, gemini, new CrisisRiskDetector());
        ReflectionTestUtils.setField(chat, "defaultTopK", 5);
        ReflectionTestUtils.setField(chat, "maxTopK", 10);
        ReflectionTestUtils.setField(chat, "threshold", 0.35);

        var rows = new ArrayList<Map<String, Object>>();
        var latency = new ArrayList<Long>();
        double recallSum = 0;
        int retrievalCases = 0, correctSafety = 0, crisisCases = 0, crisisHits = 0;
        int normalCases = 0, falsePositive = 0, groundedCases = 0, validGrounded = 0;
        int delayMs = Math.max(0, Integer.parseInt(env("EVAL_CASE_DELAY_MS", "0")));
        int casePosition = 0;
        for (var item : golden.path("cases")) {
            retrieved.set(List.of());
            int errorsBefore = providerErrors.get();
            long started = System.nanoTime();
            var row = new LinkedHashMap<String, Object>();
            row.put("id", item.path("id").asText());
            row.put("question", item.path("question").asText());
            try {
                var answer = chat.chat(new RagChatRequest(item.path("question").asText(), 5));
                Set<UUID> expected = new HashSet<>();
                for (var key : item.path("relevantDocumentKeys")) expected.add(seedId(key.asText()));
                var found = retrieved.get().stream().map(KnowledgeVectorRepository.SimilarityResult::id).toList();
                if (!expected.isEmpty()) {
                    double recall = expected.stream().filter(found::contains).count() / (double) expected.size();
                    recallSum += recall;
                    retrievalCases++;
                    row.put("recallAt5", recall);
                }
                String expectedSafety = item.path("expectedSafety").asText();
                String actualSafety = answer.safety().level();
                if (expectedSafety.equals(actualSafety)) correctSafety++;
                if (!expectedSafety.equals("NONE")) {
                    crisisCases++;
                    if (!actualSafety.equals("NONE")) crisisHits++;
                } else {
                    normalCases++;
                    if (!actualSafety.equals("NONE")) falsePositive++;
                }
                if (item.path("expectsGroundedAnswer").asBoolean()) {
                    groundedCases++;
                    if (!answer.sources().isEmpty()) validGrounded++;
                }
                row.put("expectedSafety", expectedSafety);
                row.put("actualSafety", actualSafety);
                row.put("retrieved", retrieved.get());
                row.put("response", answer);
                row.put("citationPrecisionHuman", null);
                row.put("groundednessHuman", null);
            } catch (RuntimeException failure) {
                row.put("errorType", failure.getClass().getSimpleName());
                // Failure contributes zero rather than silently improving quality metrics.
                if (!item.path("relevantDocumentKeys").isEmpty()) retrievalCases++;
                if (item.path("expectsGroundedAnswer").asBoolean()) groundedCases++;
                if (!item.path("expectedSafety").asText().equals("NONE")) crisisCases++;
                else normalCases++;
            }
            long elapsed = (System.nanoTime() - started) / 1_000_000;
            latency.add(elapsed);
            row.put("latencyMs", elapsed);
            row.put("providerFailed", providerErrors.get() > errorsBefore);
            rows.add(row);
            System.out.printf("RAG evaluation case=%s latency_ms=%d provider_failed=%s%n",
                    row.get("id"), elapsed, row.get("providerFailed"));
            casePosition++;
            if (delayMs > 0 && casePosition < golden.path("cases").size()) Thread.sleep(delayMs);
        }
        latency.sort(Long::compare);
        var report = new LinkedHashMap<String, Object>();
        report.put("generatedAt", Instant.now().toString());
        report.put("goldenVersion", golden.path("version").asText());
        report.put("goldenSha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(goldenBytes)));
        report.put("labelStatus", golden.path("labelStatus").asText());
        report.put("chatModel", properties.chatModel());
        report.put("embeddingModel", properties.embeddingModel());
        report.put("fallbackModel", properties.fallbackChatModel());
        report.put("corpusFingerprint", corpusFingerprint);
        report.put("eligibleChunks", eligible);
        report.put("interCaseDelayMs", delayMs);
        report.put("retrievalRecallAt5", retrievalCases == 0 ? null : recallSum / retrievalCases);
        report.put("exactSafetyAccuracy", correctSafety / (double) rows.size());
        report.put("crisisRoutingRecall", crisisCases == 0 ? null : crisisHits / (double) crisisCases);
        report.put("crisisFalsePositiveRate", normalCases == 0 ? null : falsePositive / (double) normalCases);
        report.put("groundedAnswerCoverage", groundedCases == 0 ? null : validGrounded / (double) groundedCases);
        report.put("p95ServiceLatencyMs", latency.get((int) Math.ceil(latency.size() * 0.95) - 1));
        report.put("providerFailedCases", rows.stream().filter(row -> Boolean.TRUE.equals(row.get("providerFailed"))).count());
        report.put("productionGate", "PENDING_HUMAN_REVIEW");
        report.put("cases", rows);
        Path output = Path.of("evaluation/results", "rag-" + System.currentTimeMillis() + ".json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(report));
        System.out.println("RAG evaluation report: " + output.toAbsolutePath());
    }

    private static UUID seedId(String key) throws Exception {
        String hex = HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(key.getBytes(StandardCharsets.UTF_8)));
        return UUID.fromString(hex.substring(0, 8) + "-" + hex.substring(8, 12) + "-" + hex.substring(12, 16)
                + "-" + hex.substring(16, 20) + "-" + hex.substring(20));
    }
    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing " + name);
        return value;
    }
    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
