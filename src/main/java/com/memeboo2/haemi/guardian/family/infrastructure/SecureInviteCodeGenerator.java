package com.memeboo2.haemi.guardian.family.infrastructure;

import com.memeboo2.haemi.guardian.family.application.InviteCodeGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** 혼동되는 문자(0/O, 1/I/L)를 제외한 8자리 코드. */
@Component
public class SecureInviteCodeGenerator implements InviteCodeGenerator {

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String nextCode() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
