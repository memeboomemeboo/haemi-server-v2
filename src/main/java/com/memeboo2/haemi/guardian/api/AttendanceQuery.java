package com.memeboo2.haemi.guardian.api;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 홈 화면에서 사용하는 어르신 출석·스트릭 조회 계약.
 *
 * <p>elder 모듈은 guardian.api만 의존할 수 있으므로, 양쪽 홈 화면이 공유하는
 * 소비자 계약을 guardian API로 공개한다. attendance 실구현은 이 계약을 구현한다.</p>
 */
public interface AttendanceQuery {

    boolean completedToday(UUID elderId);

    int currentStreak(UUID elderId);

    /** 첫 등록일부터 D+ 정수. */
    long daysTogether(UUID elderId);

    /** 보호자 홈 요일별 스택 막대용 — 최근 7일 각 날짜의 활동 종류별 완료 여부. */
    List<DayActivity> weeklyActivities(UUID elderId);

    record DayActivity(LocalDate date, DayOfWeek dayOfWeek,
                       boolean training, boolean greetingRead, boolean memoryViewed, boolean replied) {}
}
