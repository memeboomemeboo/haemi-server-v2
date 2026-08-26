package com.memeboo2.haemi.guardian.report;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.report.application.GetAttendanceDetailUseCase;
import com.memeboo2.haemi.guardian.report.application.ReportProperties;
import com.memeboo2.haemi.guardian.report.application.ReportStatusCalculator;
import com.memeboo2.haemi.guardian.report.domain.ReportParticipation;
import com.memeboo2.haemi.guardian.report.infrastructure.ReportParticipationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class GetAttendanceDetailUseCaseTest {

    @Mock CareAccessQuery careAccessQuery;
    @Mock ReportParticipationRepository participationRepository;
    @Mock HaemiClock clock;

    GetAttendanceDetailUseCase useCase;

    UUID guardianId = UUID.randomUUID();
    UUID elderId = UUID.randomUUID();
    LocalDate today = LocalDate.of(2026, 8, 25);

    @BeforeEach
    void setUp() {
        ReportProperties props = new ReportProperties(5, 3, 7, 4, 70, 40, 7, 4);
        useCase = new GetAttendanceDetailUseCase(
                careAccessQuery, participationRepository, new ReportStatusCalculator(props), props, clock);
        lenient().when(clock.today()).thenReturn(today);
    }

    @Test
    void 정상_경로_최근7일과_4주_구간을_반환한다() {
        given(participationRepository.findByElderIdAndParticipationDateGreaterThanEqual(any(), any()))
                .willReturn(List.of(
                        ReportParticipation.of(elderId, today),
                        ReportParticipation.of(elderId, today.minusDays(1))
                ));
        given(participationRepository.findByElderId(elderId)).willReturn(List.of(
                ReportParticipation.of(elderId, today),
                ReportParticipation.of(elderId, today.minusDays(1))
        ));

        var detail = useCase.execute(guardianId, elderId);

        assertThat(detail.last7Days()).hasSize(7);
        assertThat(detail.last7Days().get(6).date()).isEqualTo(today);
        assertThat(detail.last7Days().get(6).participated()).isTrue();
        assertThat(detail.last4Weeks()).hasSize(4);
        assertThat(detail.currentStreak()).isEqualTo(2);
    }

    @Test
    void 링크없는_보호자는_403() {
        willThrow(new DomainException(ErrorCode.CARE_ACCESS_DENIED))
                .given(careAccessQuery).requireGuardianOf(guardianId, elderId);

        assertThatThrownBy(() -> useCase.execute(guardianId, elderId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CARE_ACCESS_DENIED));
    }
}
