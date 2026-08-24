package com.memeboo2.haemi.elder.attendance;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.elder.attendance.application.AttendanceQueryImpl;
import com.memeboo2.haemi.elder.attendance.domain.DailyParticipation;
import com.memeboo2.haemi.elder.attendance.infrastructure.DailyParticipationRepository;
import com.memeboo2.haemi.guardian.api.ElderQuery;
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

@ExtendWith(MockitoExtension.class)
class AttendanceQueryImplTest {

    @Mock DailyParticipationRepository repository;
    @Mock ElderQuery elderQuery;
    @Mock HaemiClock clock;
    @InjectMocks AttendanceQueryImpl query;

    UUID elderId = UUID.randomUUID();
    LocalDate today = LocalDate.of(2026, 8, 25);

    @Test
    void 오늘_참여_기록이_있으면_true() {
        given(clock.today()).willReturn(today);
        given(repository.existsByElderIdAndParticipationDate(elderId, today)).willReturn(true);

        assertThat(query.completedToday(elderId)).isTrue();
    }

    @Test
    void 참여_이력으로_현재_스트릭을_계산한다() {
        given(clock.today()).willReturn(today);
        given(repository.findByElderId(elderId)).willReturn(List.of(
                DailyParticipation.of(elderId, today),
                DailyParticipation.of(elderId, today.minusDays(1))
        ));

        assertThat(query.currentStreak(elderId)).isEqualTo(2);
    }

    @Test
    void 등록일부터_지난_일수를_daysTogether로_반환한다() {
        given(clock.today()).willReturn(today);
        Instant registeredAt = today.minusDays(10).atStartOfDay(java.time.ZoneId.of("Asia/Seoul")).toInstant();
        given(elderQuery.findById(elderId))
                .willReturn(Optional.of(new ElderQuery.ElderInfo(elderId, "황정빈", registeredAt)));

        assertThat(query.daysTogether(elderId)).isEqualTo(10L);
    }

    @Test
    void 어르신_정보가_없으면_daysTogether는_0이다() {
        given(elderQuery.findById(elderId)).willReturn(Optional.empty());

        assertThat(query.daysTogether(elderId)).isEqualTo(0L);
    }
}
