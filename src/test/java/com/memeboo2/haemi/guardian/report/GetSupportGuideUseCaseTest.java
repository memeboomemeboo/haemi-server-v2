package com.memeboo2.haemi.guardian.report;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.eldermanagement.domain.Elder;
import com.memeboo2.haemi.guardian.eldermanagement.domain.ElderRepository;
import com.memeboo2.haemi.guardian.report.api.CognitiveArea;
import com.memeboo2.haemi.guardian.report.api.CognitiveStatus;
import com.memeboo2.haemi.guardian.report.api.CognitiveStatusQuery;
import com.memeboo2.haemi.guardian.report.api.CognitiveStatusQuery.AreaStatus;
import com.memeboo2.haemi.guardian.report.api.CognitiveStatusQuery.CognitiveStatusView;
import com.memeboo2.haemi.guardian.report.application.GetSupportGuideUseCase;
import com.memeboo2.haemi.guardian.report.application.ReportProperties;
import com.memeboo2.haemi.guardian.report.application.SupportGuideAction;
import com.memeboo2.haemi.guardian.report.domain.ReportParticipation;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class GetSupportGuideUseCaseTest {

    @Mock CareAccessQuery careAccessQuery;
    @Mock ElderRepository elderRepository;
    @Mock ReportParticipationRepository participationRepository;
    @Mock CognitiveStatusQuery cognitiveStatusQuery;
    @Mock HaemiClock clock;

    private final UUID guardianId = UUID.randomUUID();
    private final UUID elderId = UUID.randomUUID();
    private final LocalDate today = LocalDate.of(2026, 8, 26);
    private GetSupportGuideUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetSupportGuideUseCase(
                careAccessQuery, elderRepository, participationRepository,
                new ReportProperties(5, 3, 7, 4), cognitiveStatusQuery, clock
        );
    }

    private void stubAccessibleElder() {
        given(clock.today()).willReturn(today);
        given(elderRepository.findById(elderId)).willReturn(Optional.of(
                Elder.create(UUID.randomUUID(), UUID.randomUUID(), "황정빈", LocalDate.of(1950, 1, 1))
        ));
    }

    @Test
    void 참여가_2일_이하면_이름을_넣어_하루한마디를_제안한다() {
        stubAccessibleElder();
        given(participationRepository.findByElderIdAndParticipationDateGreaterThanEqual(elderId, today.minusDays(6)))
                .willReturn(List.of(ReportParticipation.of(elderId, today), ReportParticipation.of(elderId, today.minusDays(1))));
        given(cognitiveStatusQuery.cognitiveStatus(guardianId, elderId)).willReturn(status(
                area(CognitiveArea.ORIENTATION, CognitiveStatus.NORMAL),
                area(CognitiveArea.RECALL, CognitiveStatus.NORMAL),
                area(CognitiveArea.LANGUAGE, CognitiveStatus.NORMAL),
                area(CognitiveArea.DELAYED_RECALL, CognitiveStatus.NORMAL)
        ));

        var guide = useCase.execute(guardianId, elderId);

        assertThat(guide.suggestions()).singleElement().satisfies(suggestion -> {
            assertThat(suggestion.action()).isEqualTo(SupportGuideAction.SEND_DAILY_CARE);
            assertThat(suggestion.message()).contains("황정빈 어르신", "하루 한마디");
        });
    }

    @Test
    void 옛기억_관찰필요와_지연회상_하락을_각각_기존기능_행동으로_제안한다() {
        stubAccessibleElder();
        given(participationRepository.findByElderIdAndParticipationDateGreaterThanEqual(elderId, today.minusDays(6)))
                .willReturn(List.of(
                        ReportParticipation.of(elderId, today),
                        ReportParticipation.of(elderId, today.minusDays(1)),
                        ReportParticipation.of(elderId, today.minusDays(2))
                ));
        given(cognitiveStatusQuery.cognitiveStatus(guardianId, elderId)).willReturn(status(
                area(CognitiveArea.ORIENTATION, CognitiveStatus.NORMAL),
                area(CognitiveArea.RECALL, CognitiveStatus.WATCH),
                area(CognitiveArea.LANGUAGE, CognitiveStatus.NORMAL),
                new AreaStatus(CognitiveArea.DELAYED_RECALL, CognitiveStatus.WATCH, true)
        ));

        var guide = useCase.execute(guardianId, elderId);

        assertThat(guide.suggestions()).extracting(suggestion -> suggestion.action())
                .containsExactly(SupportGuideAction.REGISTER_MEMORY, SupportGuideAction.CALL_ELDER);
        assertThat(guide.suggestions()).allSatisfy(suggestion ->
                assertThat(suggestion.message()).contains("황정빈 어르신"));
    }

    @Test
    void 모든_인지영역이_좋음이면_칭찬을_제안한다() {
        stubAccessibleElder();
        given(participationRepository.findByElderIdAndParticipationDateGreaterThanEqual(elderId, today.minusDays(6)))
                .willReturn(List.of(
                        ReportParticipation.of(elderId, today),
                        ReportParticipation.of(elderId, today.minusDays(1)),
                        ReportParticipation.of(elderId, today.minusDays(2))
                ));
        given(cognitiveStatusQuery.cognitiveStatus(guardianId, elderId)).willReturn(status(
                area(CognitiveArea.ORIENTATION, CognitiveStatus.GOOD),
                area(CognitiveArea.RECALL, CognitiveStatus.GOOD),
                area(CognitiveArea.LANGUAGE, CognitiveStatus.GOOD),
                area(CognitiveArea.DELAYED_RECALL, CognitiveStatus.GOOD)
        ));

        var guide = useCase.execute(guardianId, elderId);

        assertThat(guide.suggestions()).singleElement().satisfies(suggestion -> {
            assertThat(suggestion.action()).isEqualTo(SupportGuideAction.PRAISE_ELDER);
            assertThat(suggestion.message()).contains("황정빈 어르신", "칭찬");
        });
    }

    @Test
    void 자동채점_결과가_없는_영역이_있으면_칭찬_제안을_만들지_않는다() {
        stubAccessibleElder();
        given(participationRepository.findByElderIdAndParticipationDateGreaterThanEqual(elderId, today.minusDays(6)))
                .willReturn(List.of(
                        ReportParticipation.of(elderId, today),
                        ReportParticipation.of(elderId, today.minusDays(1)),
                        ReportParticipation.of(elderId, today.minusDays(2))
                ));
        given(cognitiveStatusQuery.cognitiveStatus(guardianId, elderId)).willReturn(status(
                area(CognitiveArea.ORIENTATION, CognitiveStatus.GOOD),
                area(CognitiveArea.RECALL, CognitiveStatus.GOOD),
                area(CognitiveArea.LANGUAGE, CognitiveStatus.GOOD),
                area(CognitiveArea.DELAYED_RECALL, CognitiveStatus.NOT_AVAILABLE)
        ));

        var guide = useCase.execute(guardianId, elderId);

        assertThat(guide.suggestions()).isEmpty();
    }

    @Test
    void 링크없는_보호자는_상태와_어르신정보를_조회하지_않는다() {
        willThrow(new DomainException(ErrorCode.CARE_ACCESS_DENIED))
                .given(careAccessQuery).requireGuardianOf(guardianId, elderId);

        assertThatThrownBy(() -> useCase.execute(guardianId, elderId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CARE_ACCESS_DENIED));

        then(elderRepository).should(never()).findById(elderId);
        then(participationRepository).shouldHaveNoInteractions();
        then(cognitiveStatusQuery).shouldHaveNoInteractions();
    }

    private CognitiveStatusView status(AreaStatus... areas) {
        return new CognitiveStatusView(elderId, List.of(areas));
    }

    private AreaStatus area(CognitiveArea area, CognitiveStatus status) {
        return new AreaStatus(area, status, false);
    }
}
