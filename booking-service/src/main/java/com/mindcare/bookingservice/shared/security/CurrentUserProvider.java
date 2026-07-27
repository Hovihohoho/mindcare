package com.mindcare.bookingservice.shared.security;

import com.mindcare.bookingservice.shared.exception.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    public AuthenticatedUser getRequiredUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new BusinessException(
                    "AUTHENTICATION_REQUIRED",
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required");
        }
        return user;
    }

    public UUID getRequiredUserId() {
        return getRequiredUser().userId();
    }
}
