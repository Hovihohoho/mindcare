package com.mindcare.ai_service.controller;

import com.mindcare.ai_service.dto.AiConversationResponse;
import com.mindcare.ai_service.dto.ApiResponse;
import com.mindcare.ai_service.security.AuthenticatedUser;
import com.mindcare.ai_service.service.AiConversationService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/privacy")
@RequiredArgsConstructor
public class AiPrivacyController {
    private final AiConversationService conversationService;

    @GetMapping("/export")
    public ApiResponse<AiDataExport> export(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(
                "Dữ liệu AI của bạn",
                new AiDataExport(Instant.now(), conversationService.exportAll(user.userId())));
    }

    @DeleteMapping("/data")
    public ApiResponse<DeleteResult> delete(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(
                "Đã xóa dữ liệu AI",
                new DeleteResult(conversationService.deleteAll(user.userId())));
    }

    public record AiDataExport(Instant exportedAt, List<AiConversationResponse> conversations) {}
    public record DeleteResult(long deletedConversations) {}
}
