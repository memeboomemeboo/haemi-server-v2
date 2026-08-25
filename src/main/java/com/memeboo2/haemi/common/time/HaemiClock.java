package com.memeboo2.haemi.common.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public interface HaemiClock {

    ZoneId KST = ZoneId.of("Asia/Seoul");

    Instant now();

    LocalDate today();

    static LocalDate dateInKst(Instant instant) {
        return instant.atZone(KST).toLocalDate();
    }
}
