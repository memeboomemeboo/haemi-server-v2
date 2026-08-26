package com.memeboo2.haemi.guardian.report;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.report.api.CognitiveArea;
import com.memeboo2.haemi.guardian.report.api.CognitiveStatus;
import com.memeboo2.haemi.guardian.report.api.CognitiveStatusQuery;
import com.memeboo2.haemi.guardian.report.application.GetWeeklyHighlightUseCase;
import com.memeboo2.haemi.guardian.report.application.ReportProperties;
import com.memeboo2.haemi.guardian.report.application.WeeklyParticipationDaysCounter;
import com.memeboo2.haemi.guardian.report.domain.ReportParticipation;
import com.memeboo2.haemi.guardian.report.infrastructure.ReportParticipationRepository;
import com.memeboo2.haemi.platform.ai.api.WeeklyHighlightFact;
import com.memeboo2.haemi.platform.ai.api.WeeklyHighlightPrompt;
import com.memeboo2.haemi.platform.ai.api.WeeklyHighlightWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GetWeeklyHighlightUseCaseTest {

    @Mock CareAccessQuery careAccessQuery;
    @Mock CognitiveStatusQuery cognitiveStatusQuery;
    @Mock ReportParticipationRepository participationRepository;
    @Mock WeeklyHighlightWriter weeklyHighlightWriter;
    @Mock HaemiClock clock;

    private GetWeeklyHighlightUseCase useCase;
    private ReportProperties reportProperties;
    private final UUID guardianId = UUID.randomUUID();
    private final UUID elderId = UUID.randomUUID();
    private final LocalDate today = LocalDate.of(2026, 8, 26);

    @BeforeEach
    void setUp() {
        reportProperties = new ReportProperties(5, 3, 7, 4, 70, 40, 7, 4);
        useCase = new GetWeeklyHighlightUseCase(
                careAccessQuery,
                cognitiveStatusQuery,
                new WeeklyParticipationDaysCounter(participationRepository, reportProperties),
                weeklyHighlightWriter,
                clock
        );
    }

    @Test
    void 인가후_주간참여와_인지상태만_문구생성기에_전달한다() {
        given(clock.today()).willReturn(today);
        given(participationRepository.findByElderIdAndParticipationDateGreaterThanEqual(elderId, today.minusDays(6)))
                .willReturn(List.of(
                        ReportParticipation.of(elderId, today),
                        ReportParticipation.of(elderId, today.minusDays(1)),
                        ReportParticipation.of(elderId, today.minusDays(2)),
                        ReportParticipation.of(elderId, today.minusDays(3)),
                        ReportParticipation.of(elderId, today.minusDays(4))
                ));
        given(cognitiveStatusQuery.cognitiveStatus(guardianId, elderId)).willReturn(new CognitiveStatusQuery.CognitiveStatusView(
                elderId,
                List.of(
                        new CognitiveStatusQuery.AreaStatus(CognitiveArea.RECALL, CognitiveStatus.GOOD, false),
                        new CognitiveStatusQuery.AreaStatus(CognitiveArea.DELAYED_RECALL, CognitiveStatus.WATCH, true),
                        new CognitiveStatusQuery.AreaStatus(CognitiveArea.ORIENTATION, CognitiveStatus.NOT_AVAILABLE, false)
                )
        ));
        given(weeklyHighlightWriter.write(org.mockito.ArgumentMatchers.any()))
                .willReturn(List.of("이번 주 5일 참여하셨어요.", "옛 기억을 떠올리는 시간을 잘 이어가고 계세요."));

        var result = useCase.execute(guardianId, elderId);

        ArgumentCaptor<WeeklyHighlightPrompt> promptCaptor = ArgumentCaptor.forClass(WeeklyHighlightPrompt.class);
        verify(weeklyHighlightWriter).write(promptCaptor.capture());
        assertThat(promptCaptor.getValue().weeklyParticipationDays()).isEqualTo(5);
        assertThat(promptCaptor.getValue().strengths()).containsExactly(WeeklyHighlightFact.RECALL_STRENGTH);
        assertThat(promptCaptor.getValue().observations()).containsExactly(WeeklyHighlightFact.DELAYED_RECALL_SUPPORT);
        assertThat(result.lines()).hasSize(2);

        InOrder order = inOrder(careAccessQuery, cognitiveStatusQuery);
        order.verify(careAccessQuery).requireGuardianOf(guardianId, elderId);
        order.verify(cognitiveStatusQuery).cognitiveStatus(guardianId, elderId);
    }

    @Test
    void 최근4주하락도_관찰신호로_문구생성기에_전달한다() {
        given(clock.today()).willReturn(today);
        given(participationRepository.findByElderIdAndParticipationDateGreaterThanEqual(elderId, today.minusDays(6)))
                .willReturn(List.of());
        given(cognitiveStatusQuery.cognitiveStatus(guardianId, elderId)).willReturn(new CognitiveStatusQuery.CognitiveStatusView(
                elderId,
                List.of(new CognitiveStatusQuery.AreaStatus(CognitiveArea.LANGUAGE, CognitiveStatus.NORMAL, true))
        ));
        given(weeklyHighlightWriter.write(org.mockito.ArgumentMatchers.any()))
                .willReturn(List.of("말로 표현하는 일은 요즘 조금 어려워하실 수 있어요."));

        useCase.execute(guardianId, elderId);

        ArgumentCaptor<WeeklyHighlightPrompt> promptCaptor = ArgumentCaptor.forClass(WeeklyHighlightPrompt.class);
        verify(weeklyHighlightWriter).write(promptCaptor.capture());
        assertThat(promptCaptor.getValue().strengths()).isEmpty();
        assertThat(promptCaptor.getValue().observations()).containsExactly(WeeklyHighlightFact.LANGUAGE_SUPPORT);
    }

    @Test
    void 같은날의_중복_참여는_하나의_주간_참여일로_계산한다() {
        given(clock.today()).willReturn(today);
        given(participationRepository.findByElderIdAndParticipationDateGreaterThanEqual(elderId, today.minusDays(6)))
                .willReturn(List.of(
                        ReportParticipation.of(elderId, today),
                        ReportParticipation.of(elderId, today),
                        ReportParticipation.of(elderId, today.minusDays(1)),
                        ReportParticipation.of(elderId, today.minusDays(1))
                ));
        given(cognitiveStatusQuery.cognitiveStatus(guardianId, elderId)).willReturn(new CognitiveStatusQuery.CognitiveStatusView(
                elderId, List.of()
        ));
        given(weeklyHighlightWriter.write(org.mockito.ArgumentMatchers.any())).willReturn(List.of());

        useCase.execute(guardianId, elderId);

        ArgumentCaptor<WeeklyHighlightPrompt> promptCaptor = ArgumentCaptor.forClass(WeeklyHighlightPrompt.class);
        verify(weeklyHighlightWriter).write(promptCaptor.capture());
        assertThat(promptCaptor.getValue().weeklyParticipationDays()).isEqualTo(2);
    }
}
