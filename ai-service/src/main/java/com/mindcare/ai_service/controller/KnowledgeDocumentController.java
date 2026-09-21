package com.mindcare.ai_service.controller;

import com.mindcare.ai_service.dto.ApiResponse;
import com.mindcare.ai_service.dto.KnowledgeDocumentRequest;
import com.mindcare.ai_service.security.AuthenticatedUser;
import com.mindcare.ai_service.entity.KnowledgeDocument;
import com.mindcare.ai_service.service.EmbeddingService;
import com.mindcare.ai_service.service.KnowledgeDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ApiResponse<KnowledgeDocument>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "title", required = false) String title,
            @RequestPart("sourceUrl") String sourceUrl,
            @RequestPart(value = "documentType", required = false) String documentType) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đã tải tài liệu lên",
                        service.upload(file, title, sourceUrl, documentType)));
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
    ApiResponse<KnowledgeDocument> reindex(@PathVariable UUID id) {
        return ApiResponse.success("Đã tạo lại embedding", service.reindex(id));
    }

    @PostMapping("/{id}/enable")
    ApiResponse<KnowledgeDocument> enable(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success("Đã cho phép AI sử dụng tài liệu", service.enableForAi(id, user.getName()));
    }

    @PostMapping("/{id}/reject")
    ApiResponse<KnowledgeDocument> reject(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success("Đã ngừng cho AI sử dụng tài liệu", service.reject(id, user.getName()));
    }

    @PostMapping("/reindex-all")
    ApiResponse<Integer> reindexAll() {
        int count = embeddingService.reindexAll();
        return ApiResponse.success("Đã tạo lại embedding cho tài liệu", count);
    }
}
