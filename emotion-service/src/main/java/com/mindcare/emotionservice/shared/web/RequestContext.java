package com.mindcare.emotionservice.shared.web;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public final class RequestContext {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String TRACE_ID_ATTRIBUTE = RequestContext.class.getName() + ".traceId";

    private RequestContext() {
    }

    public static String traceId(HttpServletRequest request) {
        Object traceId = request.getAttribute(TRACE_ID_ATTRIBUTE);
        return traceId instanceof String value && !value.isBlank()
                ? value
                : UUID.randomUUID().toString();
    }
}
