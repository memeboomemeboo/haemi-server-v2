package com.memeboo2.haemi.common.time;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class KstClock implements HaemiClock {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Override
    public Instant now() {
        return Instant.now();
    }

    @Override
    public LocalDate today() {
        return LocalDate.now(KST);
    }
}
