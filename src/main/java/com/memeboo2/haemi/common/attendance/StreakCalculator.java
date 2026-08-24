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
     * 오늘까지의 연속 참여일. 오늘 참여가 없으면 어제부터 센다 —
     * 자정이 지나도록 참여가 없어야 그 날이 끊긴 것으로 계산되어 스트릭이 리셋된다.
     */
    public static int currentStreak(Set<LocalDate> participationDates, LocalDate today) {
        LocalDate cursor = participationDates.contains(today) ? today : today.minusDays(1);
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
