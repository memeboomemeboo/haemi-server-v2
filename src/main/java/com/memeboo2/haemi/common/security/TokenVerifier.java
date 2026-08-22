package com.memeboo2.haemi.common.security;

import java.util.Optional;

public interface TokenVerifier {
    Optional<JwtPrincipal> verify(String token);
}
