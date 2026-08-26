package com.mindcare.ai_service.controller;

import com.mindcare.ai_service.dto.AiConversationResponse;
import com.mindcare.ai_service.dto.AiConversationSummaryResponse;
import com.mindcare.ai_service.dto.ApiResponse;
import com.mindcare.ai_service.dto.RagChatRequest;
import com.mindcare.ai_service.dto.RagChatResponse;
import com.mindcare.ai_service.dto.RenameAiConversationRequest;
import com.mindcare.ai_service.security.AuthenticatedUser;
import com.mindcare.ai_service.service.AiConversationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
public class RagChatController {
    private final AiConversationService conversationService;

    @PostMapping
    public ApiResponse<RagChatResponse> chat(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody RagChatRequest request) {
        return ApiResponse.success(
                "Phản hồi từ MindCare AI",
                conversationService.chat(user.userId(), request));
    }

    @GetMapping("/conversations")
    public ApiResponse<List<AiConversationSummaryResponse>> conversations(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success("Lịch sử hội thoại", conversationService.list(user.userId()));
    }

    @GetMapping("/conversations/{conversationId}")
    public ApiResponse<AiConversationResponse> conversation(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID conversationId) {
        return ApiResponse.success(
                "Chi tiết hội thoại", conversationService.get(user.userId(), conversationId));
    }

    @PatchMapping("/conversations/{conversationId}")
    public ApiResponse<AiConversationSummaryResponse> rename(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID conversationId,
            @Valid @RequestBody RenameAiConversationRequest request) {
        return ApiResponse.success(
                "Đã đổi tên hội thoại",
                conversationService.rename(user.userId(), conversationId, request.title()));
    }

    @DeleteMapping("/conversations/{conversationId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID conversationId) {
        conversationService.delete(user.userId(), conversationId);
        return ApiResponse.success("Đã xóa hội thoại", null);
    }
}
