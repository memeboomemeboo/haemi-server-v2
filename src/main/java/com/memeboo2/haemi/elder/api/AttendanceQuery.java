package com.memeboo2.haemi.elder.api;

import java.util.UUID;

/**
 * 어르신 출석·스트릭 조회 계약.
 * 소유: elder/attendance (김연호 4단계 실구현).
 * 황정빈-5 elder/home이 소비.
 */
public interface AttendanceQuery {

    boolean completedToday(UUID elderId);

    int currentStreak(UUID elderId);

    /** 첫 등록일부터 D+ 정수. */
    long daysTogether(UUID elderId);
}
