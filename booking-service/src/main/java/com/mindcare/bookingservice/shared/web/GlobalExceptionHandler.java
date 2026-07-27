package com.mindcare.bookingservice.shared.web;

import com.mindcare.bookingservice.shared.dto.FieldErrorResponse;
import com.mindcare.bookingservice.shared.dto.ProblemResponse;
import com.mindcare.bookingservice.shared.exception.BusinessException;
import java.util.List;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemResponse> handleAccessDenied(AccessDeniedException exception) {
        return problem(
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                "You do not have permission to perform this operation",
                List.of());
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ProblemResponse> handleBusinessException(BusinessException exception) {
        return problem(exception.getStatus(), exception.getCode(), exception.getMessage(), List.of());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    ResponseEntity<ProblemResponse> handleValidation(BindException exception) {
        List<FieldErrorResponse> fields = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage()))
                .toList();
        return problem(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Request validation failed",
                fields);
    }

    @ExceptionHandler({
        ConstraintViolationException.class,
        HandlerMethodValidationException.class
    })
    ResponseEntity<ProblemResponse> handleMethodValidation(Exception exception) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Request validation failed",
                List.of());
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MissingRequestHeaderException.class,
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class
    })
    ResponseEntity<ProblemResponse> handleMalformedRequest(Exception exception) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_REQUEST",
                "The request could not be parsed",
                List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ProblemResponse> handleNoResource(NoResourceFoundException exception) {
        return problem(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "The requested resource was not found",
                List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception) {
        return problem(
                HttpStatus.METHOD_NOT_ALLOWED,
                "METHOD_NOT_ALLOWED",
                "The HTTP method is not supported for this resource",
                List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemResponse> handleUnexpected(Exception exception) {
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                List.of());
    }

    private ResponseEntity<ProblemResponse> problem(
            HttpStatus status,
            String code,
            String detail,
            List<FieldErrorResponse> fields) {
        ProblemResponse body = new ProblemResponse(
                "https://api.mindcare.vn/problems/" + code.toLowerCase().replace('_', '-'),
                status.getReasonPhrase(),
                status.value(),
                code,
                detail,
                MDC.get(CorrelationIdFilter.MDC_KEY),
                fields);
        return ResponseEntity.status(status).body(body);
    }
}
