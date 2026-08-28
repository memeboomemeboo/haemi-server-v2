package com.memeboo2.haemi.guardian.report;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.report.api.CognitiveArea;
import com.memeboo2.haemi.guardian.report.api.CognitiveStatus;
import com.memeboo2.haemi.guardian.report.api.CognitiveStatusQuery;
import com.memeboo2.haemi.guardian.report.application.GetWeeklyHighlightUseCase;
import com.memeboo2.haemi.guardian.report.application.WeeklyParticipationDaysCounter;
import com.memeboo2.haemi.guardian.report.domain.WeeklyHighlightOverride;
import com.memeboo2.haemi.guardian.report.infrastructure.WeeklyHighlightOverrideRepository;
import com.memeboo2.haemi.platform.ai.api.WeeklyHighlightFact;
import com.memeboo2.haemi.platform.ai.api.WeeklyHighlightPrompt;
import com.memeboo2.haemi.platform.ai.api.WeeklyHighlightWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GetWeeklyHighlightUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 27); // 목요일

    @Mock CareAccessQuery careAccessQuery;
    @Mock CognitiveStatusQuery cognitiveStatusQuery;
    @Mock WeeklyParticipationDaysCounter weeklyParticipationDaysCounter;
    @Mock WeeklyHighlightWriter weeklyHighlightWriter;
    @Mock WeeklyHighlightOverrideRepository overrideRepository;
    @Mock HaemiClock clock;
    @InjectMocks GetWeeklyHighlightUseCase useCase;

    private final UUID guardianId = UUID.randomUUID();
    private final UUID elderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(clock.today()).thenReturn(TODAY);
    }

    @Test
    void 보호자가_편집한_문구가_있으면_그대로_반환하고_AI를_호출하지_않는다() {
        LocalDate weekStart = TODAY.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        WeeklyHighlightOverride override = WeeklyHighlightOverride.of(
                elderId, weekStart, "직접 쓴 첫줄\n직접 쓴 둘째줄");
        given(overrideRepository.findByElderIdAndWeekStart(any(), any())).willReturn(Optional.of(override));

        GetWeeklyHighlightUseCase.WeeklyHighlight result = useCase.execute(guardianId, elderId);

        assertThat(result.items()).extracting(item -> item.body())
                .containsExactly("직접 쓴 첫줄", "직접 쓴 둘째줄");
        verifyNoInteractions(weeklyHighlightWriter, weeklyParticipationDaysCounter, cognitiveStatusQuery);
        verify(careAccessQuery).requireGuardianOf(guardianId, elderId);
    }

    @Test
    void 편집한_문구가_없으면_AI로_생성한다() {
        given(overrideRepository.findByElderIdAndWeekStart(any(), any())).willReturn(Optional.empty());
        given(weeklyParticipationDaysCounter.count(elderId, TODAY)).willReturn(4);

        CognitiveStatusQuery.CognitiveStatusView cognitive = new CognitiveStatusQuery.CognitiveStatusView(
                elderId, List.of(
                        new CognitiveStatusQuery.AreaStatus(CognitiveArea.ORIENTATION, CognitiveStatus.GOOD, false),
                        new CognitiveStatusQuery.AreaStatus(CognitiveArea.RECALL, CognitiveStatus.WATCH, false),
                        new CognitiveStatusQuery.AreaStatus(CognitiveArea.LANGUAGE, CognitiveStatus.NORMAL, true),
                        new CognitiveStatusQuery.AreaStatus(CognitiveArea.DELAYED_RECALL, CognitiveStatus.NOT_AVAILABLE, false)
                ));
        given(cognitiveStatusQuery.cognitiveStatus(guardianId, elderId)).willReturn(cognitive);
        given(weeklyHighlightWriter.write(any(WeeklyHighlightPrompt.class)))
                .willReturn(List.of("잘하고 계세요", "이 부분은 지켜봐 주세요"));

        GetWeeklyHighlightUseCase.WeeklyHighlight result = useCase.execute(guardianId, elderId);

        assertThat(result.elderId()).isEqualTo(elderId);
        assertThat(result.items()).extracting(item -> item.body())
                .containsExactly("잘하고 계세요", "이 부분은 지켜봐 주세요");

        org.mockito.ArgumentCaptor<WeeklyHighlightPrompt> captor =
                org.mockito.ArgumentCaptor.forClass(WeeklyHighlightPrompt.class);
        verify(weeklyHighlightWriter).write(captor.capture());
        WeeklyHighlightPrompt prompt = captor.getValue();

        assertThat(prompt.weeklyParticipationDays()).isEqualTo(4);
        assertThat(prompt.strengths()).containsExactly(WeeklyHighlightFact.ORIENTATION_STRENGTH);
        // RECALL은 WATCH라서, LANGUAGE는 4주간_하락이라서 둘 다 관찰 대상에 포함된다.
        assertThat(prompt.observations()).containsExactlyInAnyOrder(
                WeeklyHighlightFact.RECALL_SUPPORT, WeeklyHighlightFact.LANGUAGE_SUPPORT);
    }

    @Test
    void 자동_생성_하이라이트의_항목_ID는_같은_주에_안정적이다() {
        given(overrideRepository.findByElderIdAndWeekStart(any(), any())).willReturn(Optional.empty());
        given(weeklyParticipationDaysCounter.count(elderId, TODAY)).willReturn(4);
        given(cognitiveStatusQuery.cognitiveStatus(guardianId, elderId))
                .willReturn(new CognitiveStatusQuery.CognitiveStatusView(elderId, List.of()));
        given(weeklyHighlightWriter.write(any(WeeklyHighlightPrompt.class)))
                .willReturn(List.of("잘하고 계세요", "이 부분은 지켜봐 주세요"));

        List<UUID> firstIds = useCase.execute(guardianId, elderId).items().stream()
                .map(item -> item.id()).toList();
        List<UUID> secondIds = useCase.execute(guardianId, elderId).items().stream()
                .map(item -> item.id()).toList();

        assertThat(secondIds).containsExactlyElementsOf(firstIds);
    }

    @Test
    void 레거시_줄단위_하이라이트의_항목_ID도_같은_주에_안정적이다() {
        LocalDate weekStart = TODAY.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        WeeklyHighlightOverride override = WeeklyHighlightOverride.of(elderId, weekStart, "첫줄\n둘째줄");
        given(overrideRepository.findByElderIdAndWeekStart(any(), any())).willReturn(Optional.of(override));

        List<UUID> firstIds = useCase.execute(guardianId, elderId).items().stream()
                .map(item -> item.id()).toList();
        List<UUID> secondIds = useCase.execute(guardianId, elderId).items().stream()
                .map(item -> item.id()).toList();

        assertThat(secondIds).containsExactlyElementsOf(firstIds);
    }

    @Test
    void 인가되지_않은_접근은_예외를_전파한다() {
        org.mockito.Mockito.doThrow(new RuntimeException("접근 거부"))
                .when(careAccessQuery).requireGuardianOf(guardianId, elderId);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> useCase.execute(guardianId, elderId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("접근 거부");
    }
}
