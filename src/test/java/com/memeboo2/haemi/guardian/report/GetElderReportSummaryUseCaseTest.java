package com.memeboo2.haemi.guardian.report;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.eldermanagement.domain.Elder;
import com.memeboo2.haemi.guardian.eldermanagement.domain.ElderRepository;
import com.memeboo2.haemi.guardian.report.application.GetElderReportSummaryUseCase;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class GetElderReportSummaryUseCaseTest {

    @Mock CareAccessQuery careAccessQuery;
    @Mock ElderRepository elderRepository;
    @Mock ReportParticipationRepository participationRepository;
    @Mock HaemiClock clock;

    GetElderReportSummaryUseCase useCase;

    UUID guardianId = UUID.randomUUID();
    UUID elderId = UUID.randomUUID();
    LocalDate today = LocalDate.of(2026, 8, 25);

    @BeforeEach
    void setUp() {
        ReportProperties props = new ReportProperties(5, 3, 7, 4);
        useCase = new GetElderReportSummaryUseCase(
                careAccessQuery, elderRepository, participationRepository,
                new ReportStatusCalculator(props), props, clock);
    }

    @Test
    void 정상_경로_주5일이상_참여하면_GOOD() {
        given(clock.today()).willReturn(today);
        Instant registeredAt = today.minusDays(30).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();
        Elder elder = Elder.create(UUID.randomUUID(), UUID.randomUUID(), "황정빈", LocalDate.of(1950, 1, 1));
        ReflectionTestUtils.setField(elder, "createdAt", registeredAt);
        given(elderRepository.findById(elderId)).willReturn(Optional.of(elder));
        given(participationRepository.findByElderId(elderId)).willReturn(List.of(
                ReportParticipation.of(elderId, today),
                ReportParticipation.of(elderId, today.minusDays(1)),
                ReportParticipation.of(elderId, today.minusDays(2)),
                ReportParticipation.of(elderId, today.minusDays(3)),
                ReportParticipation.of(elderId, today.minusDays(4))
        ));

        var summary = useCase.execute(guardianId, elderId);

        assertThat(summary.weeklyParticipationDays()).isEqualTo(5);
        assertThat(summary.status()).isEqualTo(ReportStatus.GOOD);
        assertThat(summary.attendedToday()).isTrue();
        assertThat(summary.daysTogether()).isEqualTo(30L);
        assertThat(summary.currentStreak()).isEqualTo(5);
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
