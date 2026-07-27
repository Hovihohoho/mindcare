package com.mindcare.emotionservice.journal.controller;

import com.mindcare.emotionservice.journal.dto.EmotionTrendPointResponse;
import com.mindcare.emotionservice.journal.service.EmotionJournalService;
import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
import com.mindcare.emotionservice.shared.security.AuthenticatedUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping(
        path = "/api/v1/emotion-trends",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class EmotionTrendController {

    private final EmotionJournalService emotionJournalService;

    public EmotionTrendController(EmotionJournalService emotionJournalService) {
        this.emotionJournalService = emotionJournalService;
    }

    @GetMapping
    public List<EmotionTrendPointResponse> getEmotionTrends(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime to,
            @RequestParam
            String bucket,
            @RequestParam
            String timezone
    ) {
        return emotionJournalService.getEmotionTrends(
                authenticatedUser.userId(),
                from,
                to,
                bucket,
                parseTimezone(timezone)
        );
    }

    private ZoneId parseTimezone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            throw new InvalidRequestException(
                    "INVALID_TIMEZONE",
                    "timezone must be a valid IANA timezone"
            );
        }
    }
}
