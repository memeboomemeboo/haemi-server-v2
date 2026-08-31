package com.memeboo2.haemi.elder.attendance.application;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.elder.attendance.domain.DailyParticipation;
import com.memeboo2.haemi.elder.attendance.infrastructure.DailyParticipationRepository;
import com.memeboo2.haemi.guardian.api.AttendanceBadge;
import com.memeboo2.haemi.guardian.api.AttendanceQuery;
import com.memeboo2.haemi.guardian.api.ElderQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/** AttendanceQueryImplTest를 보완하는 추가 커버리지: daysTogether/weeklyActivities/배지 분기. */
@ExtendWith(MockitoExtension.class)
class AttendanceQueryImplAdditionalTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 27);

    @Mock DailyParticipationRepository repository;
    @Mock ElderQuery elderQuery;
    @Mock HaemiClock clock;
    @InjectMocks AttendanceQueryImpl query;

    private final UUID elderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(clock.today()).thenReturn(TODAY);
    }

    @Test
    void daysTogether는_등록일_기준으로_계산한다() {
        Instant registeredAt = TODAY.minusDays(10).atStartOfDay(HaemiClock.KST).toInstant();
        given(elderQuery.findById(elderId)).willReturn(
                Optional.of(new ElderQuery.ElderInfo(elderId, "김할머니", registeredAt)));

        long days = query.daysTogether(elderId);

        assertThat(days).isEqualTo(10);
    }

    @Test
    void daysTogether는_어르신을_찾지_못하면_0을_반환한다() {
        given(elderQuery.findById(elderId)).willReturn(Optional.empty());

        assertThat(query.daysTogether(elderId)).isZero();
    }

    @Test
    void weeklyActivities는_최근_7일을_날짜순으로_반환하고_미참여일은_false로_채운다() {
        DailyParticipation participated = DailyParticipation.of(elderId, TODAY);
        given(repository.findByElderIdAndParticipationDateGreaterThanEqual(elderId, TODAY.minusDays(6)))
                .willReturn(List.of(participated));

        List<AttendanceQuery.DayActivity> activities = query.weeklyActivities(elderId);

        assertThat(activities).hasSize(7);
        assertThat(activities.get(6).date()).isEqualTo(TODAY);
        assertThat(activities.get(6).training()).isFalse();
        assertThat(activities.get(0).date()).isEqualTo(TODAY.minusDays(6));
    }

    @Test
    void unlockedBadges는_누적_참여일에_따라_해금된_배지만_반환한다() {
        given(repository.countByElderId(elderId)).willReturn(30L);

        List<AttendanceBadge> badges = query.unlockedBadges(elderId);

        assertThat(badges).containsExactly(AttendanceBadge.DAYS_7, AttendanceBadge.DAYS_30);
    }

    @Test
    void unlockedBadges는_참여_기록이_없으면_빈_목록을_반환한다() {
        given(repository.countByElderId(elderId)).willReturn(0L);

        assertThat(query.unlockedBadges(elderId)).isEmpty();
    }

    @Test
    void unlockedBadgesAfterCompletion은_오늘을_제외한_참여일에_1을_더해_계산한다() {
        // 오늘 제외 6일 + 오늘 1 = 7일 → DAYS_7. 오늘 행의 커밋 가시성과 무관하게 단일 쿼리로 계산한다. (#143)
        given(repository.countByElderIdAndParticipationDateNot(elderId, TODAY)).willReturn(6L);

        List<AttendanceBadge> badges = query.unlockedBadgesAfterCompletion(elderId);

        assertThat(badges).containsExactly(AttendanceBadge.DAYS_7);
    }

    @Test
    void unlockedBadgesAfterCompletion은_오늘_행이_아직_안보여도_오늘을_한번_센다() {
        // 오늘 행이 이 읽기 스냅샷에 아직 없더라도, 오늘을 제외한 카운트에 항상 1을 더하므로 정확히 한 번 셈된다.
        given(repository.countByElderIdAndParticipationDateNot(elderId, TODAY)).willReturn(6L);

        List<AttendanceBadge> badges = query.unlockedBadgesAfterCompletion(elderId);

        assertThat(badges).containsExactly(AttendanceBadge.DAYS_7);
    }
}
