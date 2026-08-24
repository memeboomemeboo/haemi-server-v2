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
        ReportProperties props = new ReportProperties(5, 3, 7, 4);
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

        given(participationRepository.findByElderId(goodElderId)).willReturn(List.of(
                ReportParticipation.of(goodElderId, today),
                ReportParticipation.of(goodElderId, today.minusDays(1)),
                ReportParticipation.of(goodElderId, today.minusDays(2)),
                ReportParticipation.of(goodElderId, today.minusDays(3)),
                ReportParticipation.of(goodElderId, today.minusDays(4))
        ));
        given(participationRepository.findByElderId(watchElderId)).willReturn(List.of());

        var result = useCase.execute(guardianId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).elderId()).isEqualTo(watchElderId);
        assertThat(result.get(0).status()).isEqualTo(ReportStatus.WATCH);
        assertThat(result.get(1).elderId()).isEqualTo(goodElderId);
        assertThat(result.get(1).status()).isEqualTo(ReportStatus.GOOD);
    }
}
