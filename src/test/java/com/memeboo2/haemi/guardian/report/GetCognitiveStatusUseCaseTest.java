package com.memeboo2.haemi.guardian.report;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.report.api.CognitiveArea;
import com.memeboo2.haemi.guardian.report.api.CognitiveStatus;
import com.memeboo2.haemi.guardian.report.application.CognitiveStatusCalculator;
import com.memeboo2.haemi.guardian.report.application.GetCognitiveStatusUseCase;
import com.memeboo2.haemi.guardian.report.application.ReportProperties;
import com.memeboo2.haemi.guardian.report.domain.CognitiveResultSnapshot;
import com.memeboo2.haemi.guardian.report.infrastructure.CognitiveResultSnapshotRepository;
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

@ExtendWith(MockitoExtension.class)
class GetCognitiveStatusUseCaseTest {

    @Mock CareAccessQuery careAccessQuery;
    @Mock CognitiveResultSnapshotRepository snapshotRepository;
    @Mock HaemiClock clock;

    private final UUID guardianId = UUID.randomUUID();
    private final UUID elderId = UUID.randomUUID();
    private final LocalDate today = LocalDate.of(2026, 8, 26);
    private GetCognitiveStatusUseCase useCase;

    @BeforeEach
    void setUp() {
        ReportProperties properties = new ReportProperties(5, 3, 7, 4, 70, 40, 7, 4);
        useCase = new GetCognitiveStatusUseCase(
                careAccessQuery,
                snapshotRepository,
                new CognitiveStatusCalculator(properties),
                properties,
                clock);
    }

    @Test
    void 최근7일_정답률과_4주_연속하락을_영역별로_계산한다() {
        given(clock.today()).willReturn(today);
        List<CognitiveResultSnapshot> results = List.of(
                result(CognitiveArea.ORIENTATION, today.minusDays(1), 10, 8),
                result(CognitiveArea.RECALL, today.minusDays(1), 10, 5),
                result(CognitiveArea.DELAYED_RECALL, today.minusDays(1), 10, 6),
                result(CognitiveArea.DELAYED_RECALL, today.minusDays(8), 10, 7),
                result(CognitiveArea.DELAYED_RECALL, today.minusDays(15), 10, 8),
                result(CognitiveArea.DELAYED_RECALL, today.minusDays(22), 10, 9)
        );
        given(snapshotRepository.findByElderIdAndSessionDateGreaterThanEqual(any(), any())).willReturn(results);

        var view = useCase.cognitiveStatus(guardianId, elderId);

        assertThat(view.elderId()).isEqualTo(elderId);
        assertThat(view.areas()).containsExactly(
                new com.memeboo2.haemi.guardian.report.api.CognitiveStatusQuery.AreaStatus(
                        CognitiveArea.ORIENTATION, CognitiveStatus.GOOD, false),
                new com.memeboo2.haemi.guardian.report.api.CognitiveStatusQuery.AreaStatus(
                        CognitiveArea.RECALL, CognitiveStatus.NORMAL, false),
                new com.memeboo2.haemi.guardian.report.api.CognitiveStatusQuery.AreaStatus(
                        CognitiveArea.LANGUAGE, CognitiveStatus.NOT_AVAILABLE, false),
                new com.memeboo2.haemi.guardian.report.api.CognitiveStatusQuery.AreaStatus(
                        CognitiveArea.DELAYED_RECALL, CognitiveStatus.WATCH, true)
        );
    }

    @Test
    void 링크없는_보호자는_스냅샷_조회_전에_403이다() {
        willThrow(new DomainException(ErrorCode.CARE_ACCESS_DENIED))
                .given(careAccessQuery).requireGuardianOf(guardianId, elderId);

        assertThatThrownBy(() -> useCase.cognitiveStatus(guardianId, elderId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CARE_ACCESS_DENIED));
    }

    private CognitiveResultSnapshot result(CognitiveArea area, LocalDate sessionDate, int scored, int correct) {
        return CognitiveResultSnapshot.of(elderId, UUID.randomUUID(), sessionDate, area.name(), scored, correct);
    }
}
