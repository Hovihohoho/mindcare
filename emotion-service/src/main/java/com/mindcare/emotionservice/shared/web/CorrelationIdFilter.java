package com.mindcare.emotionservice.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final int MAX_CORRELATION_ID_LENGTH = 128;
    private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("[A-Za-z0-9._:-]+");
    private static final String MDC_TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = resolveTraceId(request.getHeader(RequestContext.CORRELATION_ID_HEADER));
        request.setAttribute(RequestContext.TRACE_ID_ATTRIBUTE, traceId);
        response.setHeader(RequestContext.CORRELATION_ID_HEADER, traceId);
        MDC.put(MDC_TRACE_ID, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID);
        }
    }

    private String resolveTraceId(String suppliedValue) {
        if (suppliedValue == null || suppliedValue.isBlank()) {
            return UUID.randomUUID().toString();
        }
        String normalized = suppliedValue.trim();
        if (normalized.length() > MAX_CORRELATION_ID_LENGTH
                || !SAFE_CORRELATION_ID.matcher(normalized).matches()) {
            return UUID.randomUUID().toString();
        }
        return normalized;
    }
}
