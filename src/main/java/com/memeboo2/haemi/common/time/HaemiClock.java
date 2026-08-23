package com.memeboo2.haemi.common.time;

import java.time.Instant;
import java.time.LocalDate;

public interface HaemiClock {

    Instant now();

    LocalDate today();
}
