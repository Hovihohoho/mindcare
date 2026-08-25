package com.mindcare.ai_service.controller;

import com.mindcare.ai_service.dto.ApiResponse;
import com.mindcare.ai_service.dto.SelfCareContentResponse;
import com.mindcare.ai_service.service.SelfCareContentService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/self-care-content")
@RequiredArgsConstructor
public class SelfCareContentController {
    private final SelfCareContentService service;

    @GetMapping
    ApiResponse<List<SelfCareContentResponse>> list() {
        return ApiResponse.success("Danh sách nội dung tự chăm sóc", service.list());
    }

    @GetMapping("/{id}")
    ApiResponse<SelfCareContentResponse> find(@PathVariable UUID id) {
        return ApiResponse.success("Chi tiết nội dung tự chăm sóc", service.find(id));
    }
}
