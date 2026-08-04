package com.mindcare.auth_service.controller;

import com.mindcare.auth_service.dto.ApiResponse;
import com.mindcare.auth_service.dto.UserSummary;
import com.mindcare.auth_service.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AvatarController {
    private final AccountService accountService;
    @Value("${app.upload-directory:uploads}") private String uploadDirectory;
    private static final Set<String> TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserSummary> upload(Authentication auth, @RequestPart("file") MultipartFile file)
            throws Exception {
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024 || !TYPES.contains(file.getContentType())) {
            throw new RuntimeException("Ảnh phải là JPEG, PNG hoặc WebP và không vượt quá 5MB");
        }
        String extension = switch (file.getContentType()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        Path directory = Path.of(uploadDirectory, "avatars").toAbsolutePath().normalize();
        Files.createDirectories(directory);
        String name = UUID.randomUUID() + extension;
        Files.copy(file.getInputStream(), directory.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        accountService.setAvatar(auth.getName(), "/api/auth/files/avatars/" + name);
        return ApiResponse.success("Cập nhật ảnh đại diện thành công",
                UserSummary.from(accountService.current(auth.getName())));
    }

    @GetMapping("/files/avatars/{name}")
    public ResponseEntity<Resource> avatar(@PathVariable String name) throws Exception {
        if (!name.matches("[a-f0-9-]+\\.(jpg|png|webp)")) return ResponseEntity.notFound().build();
        Path file = Path.of(uploadDirectory, "avatars", name).toAbsolutePath().normalize();
        Resource resource = new UrlResource(file.toUri());
        if (!resource.exists()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(7)))
                .contentType(MediaTypeFactory.getMediaType(name).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .body(resource);
    }
}
