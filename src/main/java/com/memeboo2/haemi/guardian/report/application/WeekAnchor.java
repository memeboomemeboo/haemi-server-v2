package com.memeboo2.haemi.guardian.report.application;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;

/** 이번 주 하이라이트 편집(#100 M5)의 주 단위 기준: 해당 날짜가 속한 ISO 주의 월요일. */
final class WeekAnchor {

    private WeekAnchor() {}

    static LocalDate of(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    static String joinLines(List<String> lines) {
        return String.join("\n", lines);
    }

    static List<String> splitLines(String content) {
        return Arrays.stream(content.split("\n", -1)).toList();
    }
}
