package com.memeboo2.haemi.common.time;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;

@Component
public class KstClock implements HaemiClock {

    @Override
    public Instant now() {
        return Instant.now();
    }

    @Override
    public LocalDate today() {
        return LocalDate.now(HaemiClock.KST);
    }
}
