package com.memeboo2.haemi.common.attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 참여 날짜 집합으로부터 스트릭을 계산하는 순수 함수 모음.
 * 저장하지 않고 조회 시마다 계산한다 (스트릭·최고 기록은 이벤트에 담긴 숫자를 복사하지 않는다).
 */
public final class StreakCalculator {

    private StreakCalculator() {}

    /**
     * 오늘까지의 연속 참여일. 오늘 참여가 없으면 0 —
     * 자정 미완료 시 리셋 (정량 명세 §4.5, v2-architecture.md §스트릭).
     */
    public static int currentStreak(Set<LocalDate> participationDates, LocalDate today) {
        if (!participationDates.contains(today)) {
            return 0;
        }
        LocalDate cursor = today;
        int streak = 0;
        while (participationDates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    /** 지금까지 기록된 참여일 중 가장 긴 연속 구간. */
    public static int bestStreak(Set<LocalDate> participationDates) {
        if (participationDates.isEmpty()) {
            return 0;
        }
        List<LocalDate> sorted = participationDates.stream().sorted().toList();
        int best = 1;
        int current = 1;
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).equals(sorted.get(i - 1).plusDays(1))) {
                current++;
            } else {
                current = 1;
            }
            best = Math.max(best, current);
        }
        return best;
    }
}
