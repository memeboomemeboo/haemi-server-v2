package com.memeboo2.haemi.guardian.report;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.eldermanagement.domain.Elder;
import com.memeboo2.haemi.guardian.eldermanagement.domain.ElderRepository;
import com.memeboo2.haemi.guardian.report.application.GetElderReportListUseCase;
import com.memeboo2.haemi.guardian.report.application.ReportProperties;
import com.memeboo2.haemi.guardian.report.application.ReportStatusCalculator;
import com.memeboo2.haemi.guardian.report.domain.ReportParticipation;
import com.memeboo2.haemi.guardian.report.domain.ReportStatus;
import com.memeboo2.haemi.guardian.report.infrastructure.ReportParticipationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GetElderReportListUseCaseTest {

    @Mock CareAccessQuery careAccessQuery;
    @Mock ElderRepository elderRepository;
    @Mock ReportParticipationRepository participationRepository;
    @Mock HaemiClock clock;

    GetElderReportListUseCase useCase;

    UUID guardianId = UUID.randomUUID();
    UUID watchElderId = UUID.randomUUID();
    UUID goodElderId = UUID.randomUUID();
    LocalDate today = LocalDate.of(2026, 8, 25);

    @BeforeEach
    void setUp() {
        ReportProperties props = new ReportProperties(5, 3, 7, 4, 70, 40, 7, 4);
        useCase = new GetElderReportListUseCase(
                careAccessQuery, elderRepository, participationRepository,
                new ReportStatusCalculator(props), props, clock);
    }

    @Test
    void 관찰필요가_좋음보다_먼저_정렬된다() {
        given(clock.today()).willReturn(today);
        given(careAccessQuery.accessibleElders(guardianId)).willReturn(List.of(goodElderId, watchElderId));

        Elder goodElder = Elder.create(UUID.randomUUID(), UUID.randomUUID(), "잘하는 어르신", null);
        Elder watchElder = Elder.create(UUID.randomUUID(), UUID.randomUUID(), "관찰필요 어르신", null);
        given(elderRepository.findById(goodElderId)).willReturn(Optional.of(goodElder));
        given(elderRepository.findById(watchElderId)).willReturn(Optional.of(watchElder));
        given(careAccessQuery.roleOf(guardianId, goodElderId)).willReturn(GuardianRole.DAUGHTER);
        given(careAccessQuery.roleOf(guardianId, watchElderId)).willReturn(GuardianRole.SON);

        LocalDate weekStart = today.minusDays(6);
        given(participationRepository.findByElderIdAndParticipationDateGreaterThanEqual(goodElderId, weekStart))
                .willReturn(List.of(
                        ReportParticipation.of(goodElderId, today),
                        ReportParticipation.of(goodElderId, today.minusDays(1)),
                        ReportParticipation.of(goodElderId, today.minusDays(2)),
                        ReportParticipation.of(goodElderId, today.minusDays(3)),
                        ReportParticipation.of(goodElderId, today.minusDays(4))
                ));
        given(participationRepository.findByElderIdAndParticipationDateGreaterThanEqual(watchElderId, weekStart))
                .willReturn(List.of());

        var result = useCase.execute(guardianId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).elderId()).isEqualTo(watchElderId);
        assertThat(result.get(0).status()).isEqualTo(ReportStatus.WATCH);
        assertThat(result.get(1).elderId()).isEqualTo(goodElderId);
        assertThat(result.get(1).status()).isEqualTo(ReportStatus.GOOD);
    }

    @Test
    void 주간_창_밖의_참여일은_주간_집계에서_제외된다() {
        given(clock.today()).willReturn(today);
        given(careAccessQuery.accessibleElders(guardianId)).willReturn(List.of(goodElderId));
        Elder elder = Elder.create(UUID.randomUUID(), UUID.randomUUID(), "어르신", null);
        given(elderRepository.findById(goodElderId)).willReturn(Optional.of(elder));
        given(careAccessQuery.roleOf(guardianId, goodElderId)).willReturn(GuardianRole.DAUGHTER);

        LocalDate weekStart = today.minusDays(6);
        // 창 안 5일 + 창 밖(미래/과거 경계) 날짜 → weeklyDays 필터 양쪽 분기 통과
        given(participationRepository.findByElderIdAndParticipationDateGreaterThanEqual(goodElderId, weekStart))
                .willReturn(List.of(
                        ReportParticipation.of(goodElderId, today),
                        ReportParticipation.of(goodElderId, today.minusDays(1)),
                        ReportParticipation.of(goodElderId, today.minusDays(2)),
                        ReportParticipation.of(goodElderId, today.minusDays(3)),
                        ReportParticipation.of(goodElderId, today.minusDays(4)),
                        ReportParticipation.of(goodElderId, today.plusDays(1)),   // isAfter(today) → 제외
                        ReportParticipation.of(goodElderId, today.minusDays(10)))); // isBefore(weekStart) → 제외

        var result = useCase.execute(guardianId);

        assertThat(result).hasSize(1);
        // 창 안 5일만 집계 → GOOD
        assertThat(result.get(0).status()).isEqualTo(ReportStatus.GOOD);
    }

    @Test
    void 조회되지_않는_어르신은_카드에서_제외된다() {
        UUID missingElderId = UUID.randomUUID();
        given(clock.today()).willReturn(today);
        given(careAccessQuery.accessibleElders(guardianId)).willReturn(List.of(missingElderId));
        given(elderRepository.findById(missingElderId)).willReturn(Optional.empty()); // elder == null 분기

        var result = useCase.execute(guardianId);

        assertThat(result).isEmpty();
    }

    @Test
    void NORMAL_상태도_정렬_우선순위에_포함된다() {
        UUID normalElderId = UUID.randomUUID();
        given(clock.today()).willReturn(today);
        given(careAccessQuery.accessibleElders(guardianId)).willReturn(List.of(goodElderId, watchElderId, normalElderId));

        Elder goodElder = Elder.create(UUID.randomUUID(), UUID.randomUUID(), "좋음", null);
        Elder watchElder = Elder.create(UUID.randomUUID(), UUID.randomUUID(), "관찰", null);
        Elder normalElder = Elder.create(UUID.randomUUID(), UUID.randomUUID(), "보통", null);
        given(elderRepository.findById(goodElderId)).willReturn(Optional.of(goodElder));
        given(elderRepository.findById(watchElderId)).willReturn(Optional.of(watchElder));
        given(elderRepository.findById(normalElderId)).willReturn(Optional.of(normalElder));
        given(careAccessQuery.roleOf(guardianId, goodElderId)).willReturn(GuardianRole.DAUGHTER);
        given(careAccessQuery.roleOf(guardianId, watchElderId)).willReturn(GuardianRole.SON);
        given(careAccessQuery.roleOf(guardianId, normalElderId)).willReturn(GuardianRole.DAUGHTER);

        LocalDate weekStart = today.minusDays(6);
        given(participationRepository.findByElderIdAndParticipationDateGreaterThanEqual(goodElderId, weekStart))
                .willReturn(List.of(
                        ReportParticipation.of(goodElderId, today),
                        ReportParticipation.of(goodElderId, today.minusDays(1)),
                        ReportParticipation.of(goodElderId, today.minusDays(2)),
                        ReportParticipation.of(goodElderId, today.minusDays(3)),
                        ReportParticipation.of(goodElderId, today.minusDays(4))));
        // NORMAL: 주 3~4일 참여 (WATCH<3, GOOD>=5 사이)
        given(participationRepository.findByElderIdAndParticipationDateGreaterThanEqual(normalElderId, weekStart))
                .willReturn(List.of(
                        ReportParticipation.of(normalElderId, today),
                        ReportParticipation.of(normalElderId, today.minusDays(1)),
                        ReportParticipation.of(normalElderId, today.minusDays(2))));
        given(participationRepository.findByElderIdAndParticipationDateGreaterThanEqual(watchElderId, weekStart))
                .willReturn(List.of());

        var result = useCase.execute(guardianId);

        // WATCH(0) → NORMAL(1) → GOOD(2) 순
        assertThat(result).extracting(GetElderReportListUseCase.Card::status)
                .containsExactly(ReportStatus.WATCH, ReportStatus.NORMAL, ReportStatus.GOOD);
    }
}
