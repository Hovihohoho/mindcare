package com.mindcare.bookingservice.chat.controller;

import com.mindcare.bookingservice.chat.dto.ConversationResponse;
import com.mindcare.bookingservice.chat.dto.MessageResponse;
import com.mindcare.bookingservice.chat.dto.SendMessageRequest;
import com.mindcare.bookingservice.chat.dto.MarkConversationReadResponse;
import com.mindcare.bookingservice.chat.service.ChatService;
import com.mindcare.bookingservice.shared.dto.CursorPageResponse;
import com.mindcare.bookingservice.shared.security.CurrentUserProvider;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAnyRole('USER','EXPERT')")
@RequiredArgsConstructor
@Validated
public class ChatController {

    private final ChatService chatService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/bookings/{bookingId}/conversation")
    public ConversationResponse getOrCreateConversation(
            @PathVariable UUID bookingId) {
        return chatService.getOrCreate(
                currentUserProvider.getRequiredUserId(),
                bookingId);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public CursorPageResponse<MessageResponse> getHistory(
            @PathVariable UUID conversationId,
            @RequestParam(required = false) @Size(max = 512) String cursor,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
        return chatService.getHistory(
                currentUserProvider.getRequiredUserId(),
                conversationId,
                cursor,
                limit);
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public MessageResponse send(
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        return chatService.send(
                currentUserProvider.getRequiredUserId(),
                conversationId,
                request);
    }

    @PostMapping("/conversations/{conversationId}:read")
    public MarkConversationReadResponse markRead(
            @PathVariable UUID conversationId) {
        int updated = chatService.markRead(
                currentUserProvider.getRequiredUserId(),
                conversationId);
        return new MarkConversationReadResponse(updated);
    }
}
