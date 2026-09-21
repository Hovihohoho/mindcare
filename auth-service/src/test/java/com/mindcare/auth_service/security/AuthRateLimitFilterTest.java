package com.mindcare.auth_service.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRateLimitFilterTest {
    @Test
    void limitsRepeatedLoginAttemptsByIp() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter();
        for (int attempt = 0; attempt < 10; attempt++) {
            MockHttpServletRequest request = loginRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse limited = new MockHttpServletResponse();
        filter.doFilter(loginRequest(), limited, new MockFilterChain());
        assertThat(limited.getStatus()).isEqualTo(429);
        assertThat(limited.getHeader("Retry-After")).isNotBlank();
    }

    private MockHttpServletRequest loginRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("192.0.2.10");
        return request;
    }
}
