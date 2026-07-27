package com.mindcare.emotionservice.shared.web;

import com.mindcare.emotionservice.shared.exception.ResourceNotFoundException;
import com.mindcare.emotionservice.shared.security.AuthenticatedUser;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityAndErrorHandlingIntegrationTest.TestEndpointConfiguration.class)
class SecurityAndErrorHandlingIntegrationTest {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String CORRELATION_ID = "test-correlation-123";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validGatewayIdentityPopulatesSecurityContext() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/test/security/me")
                        .header(USER_ID_HEADER, userId)
                        .header(RequestContext.CORRELATION_ID_HEADER, CORRELATION_ID))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestContext.CORRELATION_ID_HEADER, CORRELATION_ID))
                .andExpect(content().string(userId.toString()));
    }

    @Test
    void missingGatewayIdentityReturnsStableUnauthorizedResponse() throws Exception {
        mockMvc.perform(get("/test/security/me")
                        .header(RequestContext.CORRELATION_ID_HEADER, CORRELATION_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string(RequestContext.CORRELATION_ID_HEADER, CORRELATION_ID))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.traceId").value(CORRELATION_ID))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void malformedGatewayIdentityReturnsStableUnauthorizedResponse() throws Exception {
        mockMvc.perform(get("/test/security/me")
                        .header(USER_ID_HEADER, "not-a-uuid")
                        .header(RequestContext.CORRELATION_ID_HEADER, CORRELATION_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.detail")
                        .value("A valid authenticated user identity is required"));
    }

    @Test
    void businessExceptionUsesGlobalErrorContract() throws Exception {
        mockMvc.perform(get("/test/security/not-found")
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(RequestContext.CORRELATION_ID_HEADER, CORRELATION_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type")
                        .value("https://api.mindcare.vn/problems/resource-not-found"))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.traceId").value(CORRELATION_ID));
    }

    @Test
    void invalidRequestBodyReturnsFieldErrorsWithoutInternalDetails() throws Exception {
        mockMvc.perform(post("/test/security/validate")
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(RequestContext.CORRELATION_ID_HEADER, CORRELATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.detail").value("One or more fields are invalid"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("NOT_BLANK"));
    }

    @Test
    void unexpectedExceptionDoesNotExposeInternalDetails() throws Exception {
        mockMvc.perform(get("/test/security/unexpected")
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(RequestContext.CORRELATION_ID_HEADER, CORRELATION_ID))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.traceId").value(CORRELATION_ID));
    }

    @Test
    void missingRequiredHeaderReturnsMalformedRequest() throws Exception {
        mockMvc.perform(get("/test/security/required-header")
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(RequestContext.CORRELATION_ID_HEADER, CORRELATION_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.traceId").value(CORRELATION_ID));
    }

    @Test
    void unsupportedMethodReturns405AndAllowHeader() throws Exception {
        mockMvc.perform(post("/test/security/me")
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(RequestContext.CORRELATION_ID_HEADER, CORRELATION_ID))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "GET"))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void unsupportedContentTypeReturns415() throws Exception {
        mockMvc.perform(post("/test/security/validate")
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(RequestContext.CORRELATION_ID_HEADER, CORRELATION_ID)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("name"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void unsupportedAcceptTypeReturns406() throws Exception {
        mockMvc.perform(get("/test/security/object")
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(RequestContext.CORRELATION_ID_HEADER, CORRELATION_ID)
                        .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.code").value("NOT_ACCEPTABLE"));
    }

    @Test
    void missingResourceReturnsStable404() throws Exception {
        mockMvc.perform(get("/test/security/does-not-exist")
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(RequestContext.CORRELATION_ID_HEADER, CORRELATION_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void invalidControllerParameterReturnsValidationFieldError() throws Exception {
        mockMvc.perform(get("/test/security/limit")
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(RequestContext.CORRELATION_ID_HEADER, CORRELATION_ID)
                        .queryParam("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("limit"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("MIN"));
    }

    @Test
    void constraintViolationReturnsValidationError() throws Exception {
        mockMvc.perform(get("/test/security/constraint")
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(RequestContext.CORRELATION_ID_HEADER, CORRELATION_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestEndpointConfiguration {

        @Bean
        SecurityTestController securityTestController() {
            return new SecurityTestController();
        }
    }

    @RestController
    static class SecurityTestController {

        @GetMapping("/test/security/me")
        String currentUser(@AuthenticationPrincipal AuthenticatedUser user) {
            return user.userId().toString();
        }

        @GetMapping("/test/security/object")
        ValidationRequest object() {
            return new ValidationRequest("value");
        }

        @GetMapping("/test/security/not-found")
        String notFound() {
            throw new ResourceNotFoundException("Test resource");
        }

        @GetMapping("/test/security/unexpected")
        String unexpected() {
            throw new IllegalStateException("sensitive internal detail");
        }

        @GetMapping("/test/security/required-header")
        String requiredHeader(@RequestHeader("Idempotency-Key") String idempotencyKey) {
            return idempotencyKey;
        }

        @GetMapping("/test/security/limit")
        String limit(@RequestParam @Min(1) int limit) {
            return Integer.toString(limit);
        }

        @GetMapping("/test/security/constraint")
        String constraint() {
            throw new ConstraintViolationException(Set.of());
        }

        @PostMapping("/test/security/validate")
        String validate(@Valid @RequestBody ValidationRequest request) {
            return request.name();
        }
    }

    record ValidationRequest(@NotBlank String name) {
    }
}
