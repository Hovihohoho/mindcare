package com.mindcare.emotionservice.privacy.controller;

import com.mindcare.emotionservice.privacy.service.UserDataService;
import com.mindcare.emotionservice.privacy.service.UserDataService.UserDataExport;
import com.mindcare.emotionservice.shared.security.AuthenticatedUser;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/privacy")
public class UserDataController {
    private final UserDataService service;

    public UserDataController(UserDataService service) {
        this.service = service;
    }

    @GetMapping("/export")
    public UserDataExport export(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.export(user.userId());
    }

    @DeleteMapping("/data")
    public Map<String, Integer> delete(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.delete(user.userId());
    }
}
