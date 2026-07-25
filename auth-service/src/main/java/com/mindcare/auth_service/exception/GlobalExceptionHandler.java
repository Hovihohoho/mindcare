package com.mindcare.auth_service.exception;

import com.mindcare.auth_service.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream().findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Dữ liệu không hợp lệ");
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    @ExceptionHandler(MailException.class)
    ResponseEntity<ApiResponse<Void>> mail(MailException exception) {
        log.error("Could not send verification email through the configured SMTP server", exception);
        return ResponseEntity.status(502).body(ApiResponse.error(
                "Không thể gửi email xác thực. Vui lòng kiểm tra cấu hình SMTP hoặc mật khẩu ứng dụng Gmail"));
    }

    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<ApiResponse<Void>> business(RuntimeException exception) {
        log.warn("Request rejected: {}", exception.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(exception.getMessage()));
    }
}
