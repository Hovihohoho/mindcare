package com.mindcare.ai_service.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class GatewayAuthenticationFilterTest {

    private final GatewayAuthenticationFilter filter = new GatewayAuthenticationFilter();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesVerifiedGatewayIdentity() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "41aa1147-d62c-46f9-ae3f-83fcb76aafa7");
        request.addHeader("X-User-Role", "ROLE_ADMIN");
        var response = new MockHttpServletResponse();
        var observed = new AtomicReference<Authentication>();
        FilterChain chain = (ignoredRequest, ignoredResponse) ->
                observed.set(SecurityContextHolder.getContext().getAuthentication());

        filter.doFilter(request, response, chain);

        assertThat(observed.get()).isNotNull();
        assertThat(observed.get().getPrincipal()).isInstanceOf(AuthenticatedUser.class);
        assertThat(observed.get().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void ignoresMalformedOrUnsupportedIdentity() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "not-a-uuid");
        request.addHeader("X-User-Role", "ROLE_SUPER_ADMIN");
        var observed = new AtomicReference<Authentication>();

        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                (ignoredRequest, ignoredResponse) ->
                        observed.set(SecurityContextHolder.getContext().getAuthentication())
        );

        assertThat(observed.get()).isNull();
    }
}
