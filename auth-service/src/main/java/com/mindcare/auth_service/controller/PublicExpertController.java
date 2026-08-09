package com.mindcare.auth_service.controller;

import com.mindcare.auth_service.dto.ApiResponse;
import com.mindcare.auth_service.dto.UserSummary;
import com.mindcare.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth/public/experts")
@RequiredArgsConstructor
public class PublicExpertController {
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<UserSummary>> list(@RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.success("Danh sách chuyên gia",
                userRepository.findByExpertStatus("APPROVED",
                        PageRequest.of(0, Math.min(Math.max(limit, 1), 100),
                                Sort.by("fullName"))).stream()
                        .filter(PublicExpertController::isPublishable)
                        .map(UserSummary::from).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<UserSummary> detail(@PathVariable UUID id) {
        return ApiResponse.success("Hồ sơ chuyên gia", userRepository.findById(id)
                .filter(PublicExpertController::isPublishable)
                .map(UserSummary::from)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chuyên gia")));
    }

    private static boolean isPublishable(com.mindcare.auth_service.entity.User user) {
        return "APPROVED".equals(user.getExpertStatus())
                && Boolean.TRUE.equals(user.getIsActive())
                && Boolean.TRUE.equals(user.getEmailVerified())
                && user.getRole() != null
                && "ROLE_EXPERT".equals(user.getRole().getName());
    }
}
