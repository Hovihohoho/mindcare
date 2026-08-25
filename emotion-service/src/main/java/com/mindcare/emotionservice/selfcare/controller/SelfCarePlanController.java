package com.mindcare.emotionservice.selfcare.controller;

import com.mindcare.emotionservice.selfcare.dto.*;
import com.mindcare.emotionservice.selfcare.service.SelfCarePlanService;
import com.mindcare.emotionservice.shared.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api/v1/self-care-plan", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class SelfCarePlanController {
    private final SelfCarePlanService service;
    private final Clock clock;

    @GetMapping
    SelfCarePlanResponse get(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.get(user.userId(), LocalDate.now(clock));
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    SelfCarePlanResponse upsert(@AuthenticationPrincipal AuthenticatedUser user,
                                @Valid @RequestBody UpsertSelfCarePlanRequest request) {
        return service.upsert(user.userId(), request, LocalDate.now(clock));
    }

    @PostMapping(path = "/activities/{activityId}/completions", consumes = MediaType.APPLICATION_JSON_VALUE)
    SelfCarePlanResponse complete(@AuthenticationPrincipal AuthenticatedUser user,
                                  @PathVariable UUID activityId,
                                  @Valid @RequestBody CompleteSelfCareActivityRequest request) {
        return service.complete(user.userId(), activityId, request.completedOn(), LocalDate.now(clock));
    }

    @DeleteMapping("/activities/{activityId}/completions/{date}")
    SelfCarePlanResponse undo(@AuthenticationPrincipal AuthenticatedUser user,
                              @PathVariable UUID activityId, @PathVariable LocalDate date) {
        return service.undo(user.userId(), activityId, date, LocalDate.now(clock));
    }
}
