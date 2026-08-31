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

    /** 저장된 시각(Instant)을 KST 날짜로 변환한다. 과거 데이터 리플레이 등 발생 시각을 알 때 사용. */
    default LocalDate toLocalDate(Instant instant) {
        return dateInKst(instant);
    }
}
