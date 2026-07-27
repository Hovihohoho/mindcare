package com.mindcare.emotionservice.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindcare.emotionservice.shared.dto.ApiErrorResponse;
import com.mindcare.emotionservice.shared.web.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.List;

public class SecurityErrorResponseWriter {

    private static final String PROBLEM_BASE_URI = "https://api.mindcare.vn/problems/";

    private final ObjectMapper objectMapper;

    public SecurityErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String problemType,
            String title,
            String code,
            String detail
    ) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
                PROBLEM_BASE_URI + problemType,
                title,
                status,
                code,
                detail,
                RequestContext.traceId(request),
                List.of()
        ));
    }
}
