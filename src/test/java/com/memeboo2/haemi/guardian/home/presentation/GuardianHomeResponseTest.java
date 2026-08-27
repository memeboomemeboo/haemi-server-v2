package com.memeboo2.haemi.guardian.home.presentation;

import com.memeboo2.haemi.guardian.api.AttendanceQuery;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.home.application.GetGuardianHomeUseCase.Challenge;
import com.memeboo2.haemi.guardian.home.application.GetGuardianHomeUseCase.ElderCard;
import com.memeboo2.haemi.guardian.home.application.GetGuardianHomeUseCase.GuardianHomeData;
import com.memeboo2.haemi.guardian.home.presentation.dto.GuardianHomeResponse;
import com.memeboo2.haemi.guardian.report.api.CognitiveStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GuardianHomeResponseTest {

    @Test
    @DisplayName("GuardianHomeData로부터 어르신 카드와 챌린지를 모두 매핑한다")
    void from_전체_필드를_매핑한다() {
        UUID elderId = UUID.randomUUID();
        Instant lastLoginAt = Instant.parse("2026-08-20T00:00:00Z");
        LocalDate date = LocalDate.of(2026, 8, 20);

        AttendanceQuery.DayActivity dayActivity =
                new AttendanceQuery.DayActivity(date, DayOfWeek.THURSDAY, true, true, false, true);

        ElderCard card = new ElderCard(
                elderId, "김할머니", 82, GuardianRole.DAUGHTER, 100L, true, false,
                lastLoginAt, CognitiveStatus.GOOD, List.of(dayActivity));

        Challenge challenge = new Challenge(true, false);
        GuardianHomeData data = new GuardianHomeData(List.of(card), challenge);

        GuardianHomeResponse response = GuardianHomeResponse.from(data);

        assertThat(response.elders()).hasSize(1);
        GuardianHomeResponse.ElderCardResponse elderResponse = response.elders().get(0);
        assertThat(elderResponse.elderId()).isEqualTo(elderId);
        assertThat(elderResponse.name()).isEqualTo("김할머니");
        assertThat(elderResponse.age()).isEqualTo(82);
        assertThat(elderResponse.role()).isEqualTo(GuardianRole.DAUGHTER);
        assertThat(elderResponse.roleLabel()).isEqualTo("딸");
        assertThat(elderResponse.daysTogether()).isEqualTo(100L);
        assertThat(elderResponse.attendedToday()).isTrue();
        assertThat(elderResponse.greetingSentToday()).isFalse();
        assertThat(elderResponse.lastLoginAt()).isEqualTo(lastLoginAt);
        assertThat(elderResponse.todayCondition()).isEqualTo(CognitiveStatus.GOOD);

        assertThat(elderResponse.weeklyActivities()).hasSize(1);
        GuardianHomeResponse.DayActivityResponse dayResponse = elderResponse.weeklyActivities().get(0);
        assertThat(dayResponse.date()).isEqualTo(date);
        assertThat(dayResponse.dayOfWeek()).isEqualTo(DayOfWeek.THURSDAY);
        assertThat(dayResponse.training()).isTrue();
        assertThat(dayResponse.greetingRead()).isTrue();
        assertThat(dayResponse.memoryViewed()).isFalse();
        assertThat(dayResponse.replied()).isTrue();

        assertThat(response.challenge().greetingCompleted()).isTrue();
        assertThat(response.challenge().memoryCompleted()).isFalse();
    }

    @Test
    @DisplayName("접속 기록이 없는 어르신은 lastLoginAt이 null이다")
    void from_접속기록_없으면_lastLoginAt_null이다() {
        ElderCard card = new ElderCard(
                UUID.randomUUID(), "박할아버지", 75, GuardianRole.SON, 10L, false, false,
                null, CognitiveStatus.NOT_AVAILABLE, List.of());

        GuardianHomeData data = new GuardianHomeData(List.of(card), new Challenge(false, false));

        GuardianHomeResponse response = GuardianHomeResponse.from(data);

        assertThat(response.elders().get(0).lastLoginAt()).isNull();
        assertThat(response.elders().get(0).todayCondition()).isEqualTo(CognitiveStatus.NOT_AVAILABLE);
        assertThat(response.elders().get(0).weeklyActivities()).isEmpty();
    }

    @Test
    @DisplayName("어르신이 여러 명이면 elders 목록 순서를 유지한다")
    void from_다중_어르신_순서를_유지한다() {
        ElderCard card1 = new ElderCard(
                UUID.randomUUID(), "A", 70, GuardianRole.SON, 1L, false, false, null,
                CognitiveStatus.NOT_AVAILABLE, List.of());
        ElderCard card2 = new ElderCard(
                UUID.randomUUID(), "B", 80, GuardianRole.DAUGHTER, 2L, true, true, null,
                CognitiveStatus.NORMAL, List.of());

        GuardianHomeData data = new GuardianHomeData(List.of(card1, card2), new Challenge(true, true));

        GuardianHomeResponse response = GuardianHomeResponse.from(data);

        assertThat(response.elders()).extracting("name").containsExactly("A", "B");
    }
}
