package com.mindcare.auth_service.controller;

import com.mindcare.auth_service.dto.*;
import com.mindcare.auth_service.service.*;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/reminders") @RequiredArgsConstructor
public class ReminderController {
    private final ReminderService service; private final CurrentUserService currentUser;
    @GetMapping public ApiResponse<List<ReminderDtos.Item>> list() { return ApiResponse.success("Lịch nhắc", service.list(currentUser.id())); }
    @PutMapping("/{type}") public ApiResponse<ReminderDtos.Item> update(@PathVariable String type, @Valid @RequestBody ReminderDtos.Update request) {
        return ApiResponse.success("Đã cập nhật lịch nhắc", service.update(currentUser.id(), type, request));
    }
}
