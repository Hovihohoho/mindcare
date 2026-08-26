package com.mindcare.emotionservice.privacy.repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserDataRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public UserDataRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, List<Map<String, Object>>> export(UUID userId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource("userId", userId);
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        result.put("emotionJournals", query("SELECT * FROM emotion_schema.emotion_journals WHERE user_id = :userId ORDER BY created_at", parameters));
        result.put("healthMetrics", query("SELECT * FROM emotion_schema.health_metrics WHERE user_id = :userId ORDER BY recorded_at", parameters));
        result.put("healthSourceConsents", query("SELECT * FROM emotion_schema.health_source_consents WHERE user_id = :userId ORDER BY created_at", parameters));
        result.put("assessmentResults", query("SELECT * FROM emotion_schema.assessment_results WHERE user_id = :userId ORDER BY created_at", parameters));
        result.put("riskAlerts", query("SELECT * FROM emotion_schema.psychological_alert_logs WHERE user_id = :userId ORDER BY created_at", parameters));
        result.put("selfCarePlans", query("SELECT * FROM emotion_schema.self_care_plans WHERE user_id = :userId ORDER BY created_at", parameters));
        result.put("selfCareActivities", query("SELECT a.* FROM emotion_schema.self_care_activities a JOIN emotion_schema.self_care_plans p ON p.id = a.plan_id WHERE p.user_id = :userId ORDER BY a.display_order", parameters));
        result.put("selfCareCompletions", query("SELECT c.* FROM emotion_schema.self_care_completions c JOIN emotion_schema.self_care_activities a ON a.id = c.activity_id JOIN emotion_schema.self_care_plans p ON p.id = a.plan_id WHERE p.user_id = :userId ORDER BY c.completed_on", parameters));
        return result;
    }

    public Map<String, Integer> delete(UUID userId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource("userId", userId);
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("selfCareCompletions", update("DELETE FROM emotion_schema.self_care_completions c USING emotion_schema.self_care_activities a, emotion_schema.self_care_plans p WHERE c.activity_id = a.id AND a.plan_id = p.id AND p.user_id = :userId", parameters));
        counts.put("selfCareActivities", update("DELETE FROM emotion_schema.self_care_activities a USING emotion_schema.self_care_plans p WHERE a.plan_id = p.id AND p.user_id = :userId", parameters));
        counts.put("selfCarePlans", update("DELETE FROM emotion_schema.self_care_plans WHERE user_id = :userId", parameters));
        counts.put("riskAlerts", update("DELETE FROM emotion_schema.psychological_alert_logs WHERE user_id = :userId", parameters));
        counts.put("assessmentResults", update("DELETE FROM emotion_schema.assessment_results WHERE user_id = :userId", parameters));
        counts.put("healthSyncRequests", update("DELETE FROM emotion_schema.health_metric_sync_requests WHERE user_id = :userId", parameters));
        counts.put("healthMetrics", update("DELETE FROM emotion_schema.health_metrics WHERE user_id = :userId", parameters));
        counts.put("healthSourceConsents", update("DELETE FROM emotion_schema.health_source_consents WHERE user_id = :userId", parameters));
        counts.put("emotionJournals", update("DELETE FROM emotion_schema.emotion_journals WHERE user_id = :userId", parameters));
        return counts;
    }

    private List<Map<String, Object>> query(String sql, MapSqlParameterSource parameters) {
        return jdbc.queryForList(sql, parameters);
    }

    private int update(String sql, MapSqlParameterSource parameters) {
        return jdbc.update(sql, parameters);
    }
}
