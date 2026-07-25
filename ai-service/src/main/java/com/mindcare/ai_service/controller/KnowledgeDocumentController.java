package com.mindcare.ai_service.controller;

import com.mindcare.ai_service.dto.ApiResponse;
import com.mindcare.ai_service.dto.KnowledgeDocumentRequest;
import com.mindcare.ai_service.entity.KnowledgeDocument;
import com.mindcare.ai_service.service.KnowledgeDocumentService;
import com.mindcare.ai_service.service.EmbeddingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai/documents")
@RequiredArgsConstructor
public class KnowledgeDocumentController {
    private final KnowledgeDocumentService service;
    private final EmbeddingService embeddingService;

    @GetMapping
    ApiResponse<List<KnowledgeDocument>> findAll() {
        return ApiResponse.success("Danh sách tài liệu AI", service.findAll());
    }

    @GetMapping("/{id}")
    ApiResponse<KnowledgeDocument> findById(@PathVariable UUID id) {
        return ApiResponse.success("Chi tiết tài liệu AI", service.findById(id));
    }

    @PostMapping
    ResponseEntity<ApiResponse<KnowledgeDocument>> create(
            @Valid @RequestBody KnowledgeDocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo tài liệu thành công", service.create(request)));
    }

    @PutMapping("/{id}")
    ApiResponse<KnowledgeDocument> update(@PathVariable UUID id,
                                          @Valid @RequestBody KnowledgeDocumentRequest request) {
        return ApiResponse.success("Cập nhật tài liệu thành công", service.update(id, request));
    }

    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.success("Xóa tài liệu thành công", null);
    }

    @PostMapping("/{id}/reindex")
    ApiResponse<Void> reindex(@PathVariable UUID id) {
        embeddingService.reindex(id);
        return ApiResponse.success("Đã tạo lại embedding", null);
    }

    @PostMapping("/reindex-all")
    ApiResponse<Integer> reindexAll() {
        int count = embeddingService.reindexAll();
        return ApiResponse.success("Đã tạo lại embedding cho tài liệu", count);
    }
}
