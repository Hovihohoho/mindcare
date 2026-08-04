package com.mindcare.auth_service.controller;

import com.mindcare.auth_service.dto.ApiResponse;
import com.mindcare.auth_service.dto.BookmarkDtos;
import com.mindcare.auth_service.entity.Bookmark;
import com.mindcare.auth_service.repository.BookmarkRepository;
import com.mindcare.auth_service.service.CurrentUserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {
    private final BookmarkRepository repository;
    private final CurrentUserService currentUser;

    @GetMapping
    public ApiResponse<List<BookmarkDtos.Item>> list() {
        return ApiResponse.success("Danh sách nội dung đã lưu",
                repository.findByUserIdOrderByCreatedAtDesc(currentUser.id()).stream()
                        .map(BookmarkDtos.Item::from).toList());
    }

    @PostMapping
    public ApiResponse<BookmarkDtos.Item> create(@Valid @RequestBody BookmarkDtos.Create request) {
        Bookmark value = repository.findByUserIdAndTargetTypeAndTargetId(
                currentUser.id(), request.targetType(), request.targetId())
                .orElseGet(() -> repository.save(Bookmark.create(
                        currentUser.id(), request.targetType(), request.targetId())));
        return ApiResponse.success("Đã lưu nội dung", BookmarkDtos.Item.from(value));
    }

    @DeleteMapping
    public ApiResponse<Void> remove(@RequestParam String targetType, @RequestParam String targetId) {
        com.mindcare.auth_service.entity.BookmarkType type;
        try {
            type = com.mindcare.auth_service.entity.BookmarkType.valueOf(targetType);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        Bookmark value = repository.findByUserIdAndTargetTypeAndTargetId(currentUser.id(), type, targetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        repository.delete(value);
        return ApiResponse.success("Đã bỏ lưu nội dung", null);
    }
}
