package com.memeboo2.haemi.auth.verification.infrastructure;

import com.memeboo2.haemi.auth.verification.application.VerificationCodeGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class SecureVerificationCodeGenerator implements VerificationCodeGenerator {

    private final SecureRandom random = new SecureRandom();

    @Override
    public String nextCode() {
        return "%06d".formatted(random.nextInt(1_000_000));
    }
}
