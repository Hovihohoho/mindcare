package com.mindcare.emotionservice.journal.controller;

import com.mindcare.emotionservice.journal.dto.CreateEmotionJournalRequest;
import com.mindcare.emotionservice.journal.dto.EmotionJournalResponse;
import com.mindcare.emotionservice.journal.service.EmotionJournalService;
import com.mindcare.emotionservice.shared.dto.CursorPageResponse;
import com.mindcare.emotionservice.shared.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping(
        path = EmotionJournalController.BASE_PATH,
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class EmotionJournalController {

    static final String BASE_PATH = "/api/v1/emotion-journals";

    private final EmotionJournalService emotionJournalService;

    public EmotionJournalController(EmotionJournalService emotionJournalService) {
        this.emotionJournalService = emotionJournalService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EmotionJournalResponse> createJournal(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateEmotionJournalRequest request
    ) {
        EmotionJournalResponse response =
                emotionJournalService.createJournal(authenticatedUser.userId(), request);
        URI location = URI.create(BASE_PATH + "/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{journalId}")
    public EmotionJournalResponse getJournal(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID journalId
    ) {
        return emotionJournalService.getJournal(authenticatedUser.userId(), journalId);
    }

    @GetMapping
    public CursorPageResponse<EmotionJournalResponse> getJournalHistory(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime to,
            @RequestParam(required = false)
            String cursor,
            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            int limit
    ) {
        return emotionJournalService.getJournalHistory(
                authenticatedUser.userId(),
                from,
                to,
                cursor,
                limit
        );
    }

    @DeleteMapping("/{journalId}")
    public ResponseEntity<Void> deleteJournal(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID journalId
    ) {
        emotionJournalService.deleteJournal(authenticatedUser.userId(), journalId);
        return ResponseEntity.noContent().build();
    }
}
