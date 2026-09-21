package com.mindcare.emotionservice.reminder.controller;

import java.time.*;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/internal/reminder-status")
public class InternalReminderStatusController {
    private final JdbcTemplate jdbc;
    private final String internalSecret;
    public InternalReminderStatusController(JdbcTemplate jdbc, @Value("${app.internal-secret}") String internalSecret) { this.jdbc = jdbc; this.internalSecret = internalSecret; }
    public record Status(boolean checkedInToday, boolean selfCareCompletedToday) {}
    @GetMapping("/{userId}")
    public Status status(@RequestHeader("X-Internal-Secret") String supplied, @PathVariable UUID userId, @RequestParam(defaultValue="Asia/Ho_Chi_Minh") String timezone) {
        if (!internalSecret.equals(supplied)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        ZoneId zone; try { zone = ZoneId.of(timezone); } catch (DateTimeException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid timezone"); }
        LocalDate today = LocalDate.now(zone); OffsetDateTime from = today.atStartOfDay(zone).toOffsetDateTime(); OffsetDateTime to = today.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        Boolean checkedIn = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM emotion_schema.emotion_journals WHERE user_id=? AND deleted_at IS NULL AND created_at>=? AND created_at<?)", Boolean.class, userId, from, to);
        Boolean selfCare = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM emotion_schema.self_care_completions c JOIN emotion_schema.self_care_activities a ON a.id=c.activity_id JOIN emotion_schema.self_care_plans p ON p.id=a.plan_id WHERE p.user_id=? AND c.completed_on=?)", Boolean.class, userId, today);
        return new Status(Boolean.TRUE.equals(checkedIn), Boolean.TRUE.equals(selfCare));
    }
}
