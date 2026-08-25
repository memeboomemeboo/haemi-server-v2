package com.memeboo2.haemi.elder.attendance.application;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.elder.attendance.infrastructure.DailyParticipationRepository;
import com.memeboo2.haemi.guardian.api.AttendanceBadge;
import com.memeboo2.haemi.guardian.api.ElderProfileQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/** 출석은 완료 이벤트의 읽기 모델이며 스트릭·배지는 조회 시 계산한다. */
@ExtendWith(MockitoExtension.class)
class AttendanceQueryImplTest {

    @Mock DailyParticipationRepository participationRepository;
    @Mock ElderProfileQuery elderProfileQuery;
    @Mock HaemiClock clock;

    @Test
    void 오늘_기록이_없으면_스트릭은_자정에_즉시_0이_되고_누적_배지는_유지된다() {
        UUID elderId = UUID.randomUUID();
        LocalDate today = LocalDate.of(2026, 8, 25);
        given(clock.today()).willReturn(today);
        given(participationRepository.findParticipationDatesThrough(elderId, today))
                .willReturn(List.of(today.minusDays(1), today.minusDays(2)));
        given(participationRepository.countByElderId(elderId)).willReturn(30L);
        AttendanceQueryImpl query = new AttendanceQueryImpl(participationRepository, elderProfileQuery, clock);

        assertThat(query.currentStreak(elderId)).isZero();
        assertThat(query.unlockedBadges(elderId)).containsExactly(AttendanceBadge.DAYS_7, AttendanceBadge.DAYS_30);
    }

    @Test
    void 연속_참여일은_오늘부터_끊기지_않은_날짜만_센다() {
        UUID elderId = UUID.randomUUID();
        LocalDate today = LocalDate.of(2026, 8, 25);
        given(clock.today()).willReturn(today);
        given(participationRepository.findParticipationDatesThrough(elderId, today))
                .willReturn(List.of(today, today.minusDays(1), today.minusDays(2), today.minusDays(4)));
        AttendanceQueryImpl query = new AttendanceQueryImpl(participationRepository, elderProfileQuery, clock);

        assertThat(query.currentStreak(elderId)).isEqualTo(3);
    }

    @Test
    void 완료_응답은_이벤트_소비_전에도_방금_해금된_배지를_보여준다() {
        UUID elderId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        given(participationRepository.countByElderId(elderId)).willReturn(6L);
        given(participationRepository.existsByTrainingSessionId(sessionId)).willReturn(false);
        AttendanceQueryImpl query = new AttendanceQueryImpl(participationRepository, elderProfileQuery, clock);

        assertThat(query.unlockedBadgesAfterCompletion(elderId, sessionId)).containsExactly(AttendanceBadge.DAYS_7);
    }
}
