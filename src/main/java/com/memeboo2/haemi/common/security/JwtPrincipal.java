package com.memeboo2.haemi.common.security;

import java.util.UUID;

public record JwtPrincipal(UUID userId, String role) {

    public boolean isGuardian() {
        return "ROLE_GUARDIAN".equals(role);
    }

    public boolean isElder() {
        return "ROLE_ELDER".equals(role);
    }
}
