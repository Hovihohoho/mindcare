package com.mindcare.emotionservice.shared.web;

import com.mindcare.emotionservice.shared.dto.ApiErrorResponse;
import com.mindcare.emotionservice.shared.dto.FieldErrorResponse;
import com.mindcare.emotionservice.shared.exception.BusinessException;
import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
import com.mindcare.emotionservice.shared.exception.ResourceConflictException;
import com.mindcare.emotionservice.shared.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String PROBLEM_BASE_URI = "https://api.mindcare.vn/problems/";

    @ExceptionHandler(InvalidRequestException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidRequest(
            InvalidRequestException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                "invalid-request",
                "Invalid request",
                exception.getCode(),
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.NOT_FOUND,
                "resource-not-found",
                "Resource not found",
                exception.getCode(),
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(ResourceConflictException.class)
    ResponseEntity<ApiErrorResponse> handleConflict(
            ResourceConflictException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.CONFLICT,
                "resource-conflict",
                "Resource conflict",
                exception.getCode(),
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleBodyValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<FieldErrorResponse> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .toList();
        return validationResponse(request, fieldErrors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiErrorResponse> handleMethodValidation(
            HandlerMethodValidationException exception,
            HttpServletRequest request
    ) {
        List<FieldErrorResponse> fieldErrors = exception.getParameterValidationResults()
                .stream()
                .flatMap(result -> result.getResolvableErrors().stream().map(error ->
                        new FieldErrorResponse(
                                parameterName(result.getMethodParameter().getParameterName()),
                                normalizeCode(lastCode(error.getCodes()))
                        )))
                .toList();
        return validationResponse(request, fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<FieldErrorResponse> fieldErrors = exception.getConstraintViolations()
                .stream()
                .map(this::toFieldError)
                .toList();
        return validationResponse(request, fieldErrors);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            ServletRequestBindingException.class,
            TypeMismatchException.class
    })
    ResponseEntity<ApiErrorResponse> handleMalformedRequest(
            Exception exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                "malformed-request",
                "Malformed request",
                "MALFORMED_REQUEST",
                "The request syntax or parameter format is invalid",
                request,
                List.of()
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        String traceId = RequestContext.traceId(request);
        Set<HttpMethod> supportedMethods = exception.getSupportedHttpMethods();
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .header(RequestContext.CORRELATION_ID_HEADER, traceId)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON);
        if (supportedMethods != null && !supportedMethods.isEmpty()) {
            builder.allow(supportedMethods.toArray(HttpMethod[]::new));
        }
        return builder.body(error(
                HttpStatus.METHOD_NOT_ALLOWED,
                "method-not-allowed",
                "Method not allowed",
                "METHOD_NOT_ALLOWED",
                "The HTTP method is not supported for this resource",
                traceId,
                List.of()
        ));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "unsupported-media-type",
                "Unsupported media type",
                "UNSUPPORTED_MEDIA_TYPE",
                "The request Content-Type is not supported",
                request,
                List.of()
        );
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    ResponseEntity<ApiErrorResponse> handleNotAcceptable(
            HttpMediaTypeNotAcceptableException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.NOT_ACCEPTABLE,
                "not-acceptable",
                "Not acceptable",
                "NOT_ACCEPTABLE",
                "The requested response media type is not supported",
                request,
                List.of()
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiErrorResponse> handleNoResourceFound(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.NOT_FOUND,
                "resource-not-found",
                "Resource not found",
                "RESOURCE_NOT_FOUND",
                "The requested resource was not found",
                request,
                List.of()
        );
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiErrorResponse> handleBusinessRule(
            BusinessException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "business-rule-violation",
                "Business rule violation",
                exception.getCode(),
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        String traceId = RequestContext.traceId(request);
        LOGGER.error(
                "event=unhandled_request_error traceId={} exceptionType={}",
                traceId,
                exception.getClass().getName(),
                exception
        );
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "internal-server-error",
                "Internal server error",
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                traceId,
                List.of()
        );
    }

    private ResponseEntity<ApiErrorResponse> validationResponse(
            HttpServletRequest request,
            List<FieldErrorResponse> fieldErrors
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                "validation-error",
                "Request validation failed",
                "VALIDATION_ERROR",
                "One or more fields are invalid",
                request,
                fieldErrors
        );
    }

    private FieldErrorResponse toFieldError(FieldError error) {
        return new FieldErrorResponse(error.getField(), normalizeCode(error.getCode()));
    }

    private FieldErrorResponse toFieldError(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int separator = path.lastIndexOf('.');
        String field = separator >= 0 ? path.substring(separator + 1) : path;
        if (field.isBlank()) {
            field = "request";
        }
        String code = violation.getConstraintDescriptor()
                .getAnnotation()
                .annotationType()
                .getSimpleName();
        return new FieldErrorResponse(field, normalizeCode(code));
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            return "INVALID_VALUE";
        }
        return code.replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);
    }

    private String parameterName(String parameterName) {
        return parameterName == null || parameterName.isBlank() ? "request" : parameterName;
    }

    private String lastCode(String[] codes) {
        return codes == null || codes.length == 0 ? null : codes[codes.length - 1];
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status,
            String problemType,
            String title,
            String code,
            String detail,
            HttpServletRequest request,
            List<FieldErrorResponse> fieldErrors
    ) {
        return response(
                status,
                problemType,
                title,
                code,
                detail,
                RequestContext.traceId(request),
                fieldErrors
        );
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status,
            String problemType,
            String title,
            String code,
            String detail,
            String traceId,
            List<FieldErrorResponse> fieldErrors
    ) {
        return ResponseEntity.status(status)
                .header(RequestContext.CORRELATION_ID_HEADER, traceId)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(error(status, problemType, title, code, detail, traceId, fieldErrors));
    }

    private ApiErrorResponse error(
            HttpStatus status,
            String problemType,
            String title,
            String code,
            String detail,
            String traceId,
            List<FieldErrorResponse> fieldErrors
    ) {
        return new ApiErrorResponse(
                PROBLEM_BASE_URI + problemType,
                title,
                status.value(),
                code,
                detail,
                traceId,
                fieldErrors
        );
    }
}
