package com.memeboo2.haemi.guardian.api;

/** 누적 참여일로 자동 해금되는 CIST 출석 배지다. */
public enum AttendanceBadge {
    DAYS_7(7),
    DAYS_30(30),
    DAYS_100(100);

    private final long requiredDays;

    AttendanceBadge(long requiredDays) {
        this.requiredDays = requiredDays;
    }

    public boolean isUnlockedBy(long participationDays) {
        return participationDays >= requiredDays;
    }
}
