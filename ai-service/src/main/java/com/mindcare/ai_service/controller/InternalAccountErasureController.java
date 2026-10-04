package com.mindcare.ai_service.controller;

import com.mindcare.ai_service.service.AccountErasureService;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai/internal/privacy")
public class InternalAccountErasureController {
    private final AccountErasureService erasure;
    private final String secret;
    public InternalAccountErasureController(AccountErasureService erasure,
            @Value("${app.internal-secret:local-internal-secret}") String secret) {
        this.erasure = erasure;
        this.secret = secret;
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void erase(@PathVariable UUID userId, @RequestHeader("X-Internal-Secret") String supplied) {
        if (!secret.equals(supplied)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        erasure.erase(userId);
    }
}
