package com.memeboo2.haemi.common.attendance;

import com.memeboo2.haemi.common.time.HaemiClock;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * "함께한 일 수" 계산기 — 첫 등록 시각(KST 날짜)부터 오늘까지의 D+ 정수.
 * KST 존은 {@link HaemiClock#ZONE} 한 곳에서만 관리한다 (중복 하드코딩 제거).
 */
public final class DaysTogetherCalculator {

    private DaysTogetherCalculator() {}

    public static long daysTogether(Instant registeredAt, LocalDate today) {
        LocalDate registeredDate = registeredAt.atZone(HaemiClock.ZONE).toLocalDate();
        return ChronoUnit.DAYS.between(registeredDate, today);
    }
}
