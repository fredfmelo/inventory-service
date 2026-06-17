package com.fredfmelo.inventoryservice.security;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;

@Component
public class UserContextExtractor {

    static final String HEADER_USER_ID = "X-User-Id";
    static final String HEADER_USER_ROLE = "X-User-Role";

    public UserContext extract(NativeWebRequest request) {
        String userIdHeader = request.getHeader(HEADER_USER_ID);
        String roleHeader = request.getHeader(HEADER_USER_ROLE);

        if (userIdHeader == null || userIdHeader.isBlank()) {
            return null;
        }

        UUID userId = UUID.fromString(userIdHeader.trim());
        Role role = null;
        if (roleHeader != null && !roleHeader.isBlank()) {
            try {
                role = Role.valueOf(roleHeader.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // unrecognised role → treated as no role, service checks will return 403
            }
        }

        return new UserContext(userId, role);
    }
}
