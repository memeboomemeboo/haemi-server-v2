package com.memeboo2.haemi.auth.credential;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String encode(String raw) {
        return encoder.encode(raw);
    }

    public boolean matches(String raw, String hash) {
        return encoder.matches(raw, hash);
    }
}
