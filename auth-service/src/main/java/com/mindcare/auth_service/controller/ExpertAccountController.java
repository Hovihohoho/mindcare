package com.mindcare.auth_service.controller;

import com.mindcare.auth_service.dto.AccountRequests;
import com.mindcare.auth_service.dto.ApiResponse;
import com.mindcare.auth_service.dto.UserSummary;
import com.mindcare.auth_service.entity.ExpertDocument;
import com.mindcare.auth_service.entity.User;
import com.mindcare.auth_service.repository.ExpertDocumentRepository;
import com.mindcare.auth_service.repository.UserRepository;
import com.mindcare.auth_service.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth/expert-profile")
@RequiredArgsConstructor
public class ExpertAccountController {
    private static final Set<String> DOCUMENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf");
    private final AccountService accountService;
    private final UserRepository userRepository;
    private final ExpertDocumentRepository documentRepository;
    @Value("${app.upload-directory:uploads}") private String uploadDirectory;

    @GetMapping("/me")
    public ApiResponse<ExpertProfileResponse> me(Authentication authentication) {
        return ApiResponse.success("Hồ sơ chuyên gia", response(accountService.current(authentication.getName())));
    }

    @PostMapping("/submit")
    public ApiResponse<ExpertProfileResponse> submit(Authentication authentication) {
        User user = accountService.current(authentication.getName());
        if (user.getHeadline() == null || user.getHeadline().isBlank()
                || user.getSpecialties() == null || user.getSpecialties().isBlank()
                || user.getYearsOfExperience() == null || user.getConsultationFee() == null) {
            throw new RuntimeException("Vui lòng hoàn thiện chuyên môn, kinh nghiệm và giá tư vấn");
        }
        if (documentRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).isEmpty()) {
            throw new RuntimeException("Vui lòng thêm ít nhất một bằng cấp hoặc chứng chỉ");
        }
        user.setExpertStatus("PENDING");
        user.setExpertReviewReason(null);
        user.setExpertSubmittedAt(OffsetDateTime.now());
        return ApiResponse.success("Hồ sơ đã được gửi duyệt", response(userRepository.save(user)));
    }

    @PostMapping("/documents")
    public ApiResponse<DocumentResponse> addDocument(Authentication authentication,
            @Valid @RequestBody AccountRequests.ExpertDocumentRequest request) {
        ExpertDocument item = new ExpertDocument();
        item.setUser(accountService.current(authentication.getName()));
        item.setDocumentType(request.documentType());
        item.setTitle(request.title());
        item.setFileUrl(request.fileUrl());
        return ApiResponse.success("Đã thêm tài liệu", DocumentResponse.from(documentRepository.save(item)));
    }

    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DocumentResponse> uploadDocument(
            Authentication authentication, @RequestPart("file") MultipartFile file,
            @RequestPart("documentType") String documentType, @RequestPart("title") String title)
            throws Exception {
        if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024
                || !DOCUMENT_TYPES.contains(file.getContentType())) {
            throw new RuntimeException("Tài liệu phải là JPEG, PNG, WebP hoặc PDF và không vượt quá 10MB");
        }
        if (title.isBlank() || title.length() > 255 || documentType.isBlank()
                || documentType.length() > 50) {
            throw new RuntimeException("Loại và tên tài liệu không hợp lệ");
        }
        ExpertDocument item = new ExpertDocument();
        item.setUser(accountService.current(authentication.getName()));
        item.setDocumentType(documentType.trim().toUpperCase());
        item.setTitle(title.trim());
        item = documentRepository.save(item);
        String extension = switch (file.getContentType()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "application/pdf" -> ".pdf";
            default -> ".jpg";
        };
        Files.createDirectories(documentDirectory());
        Files.copy(file.getInputStream(), documentDirectory().resolve(item.getId() + extension),
                StandardCopyOption.REPLACE_EXISTING);
        item.setFileUrl("/api/auth/expert-profile/documents/" + item.getId() + "/file");
        return ApiResponse.success("Đã tải tài liệu lên", DocumentResponse.from(documentRepository.save(item)));
    }

    @GetMapping("/documents/{id}/file")
    public ResponseEntity<Resource> documentFile(Authentication authentication, @PathVariable UUID id)
            throws Exception {
        User requester = accountService.current(authentication.getName());
        ExpertDocument item = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));
        boolean admin = "ROLE_ADMIN".equals(requester.getRole().getName());
        if (!admin && !item.getUser().getId().equals(requester.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        for (String extension : List.of(".jpg", ".png", ".webp", ".pdf")) {
            Path path = documentDirectory().resolve(id + extension);
            if (Files.exists(path)) {
                Resource resource = new UrlResource(path.toUri());
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + id + extension + "\"")
                        .contentType(MediaTypeFactory.getMediaType(path.getFileName().toString())
                                .orElse(MediaType.APPLICATION_OCTET_STREAM))
                        .body(resource);
            }
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/documents/{id}")
    public ApiResponse<Void> deleteDocument(Authentication authentication, @PathVariable UUID id)
            throws Exception {
        User user = accountService.current(authentication.getName());
        ExpertDocument item = documentRepository.findById(id)
                .filter(document -> document.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));
        documentRepository.delete(item);
        for (String extension : List.of(".jpg", ".png", ".webp", ".pdf")) {
            Files.deleteIfExists(documentDirectory().resolve(id + extension));
        }
        return ApiResponse.success("Đã xóa tài liệu", null);
    }

    private Path documentDirectory() {
        return Path.of(uploadDirectory, "expert-documents").toAbsolutePath().normalize();
    }

    private ExpertProfileResponse response(User user) {
        return new ExpertProfileResponse(UserSummary.from(user),
                documentRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                        .map(DocumentResponse::from).toList());
    }

    public record ExpertProfileResponse(UserSummary profile, List<DocumentResponse> documents) {}
    public record DocumentResponse(UUID id, String documentType, String title,
                                   String fileUrl, OffsetDateTime createdAt) {
        static DocumentResponse from(ExpertDocument item) {
            return new DocumentResponse(item.getId(), item.getDocumentType(), item.getTitle(),
                    item.getFileUrl(), item.getCreatedAt());
        }
    }
}
