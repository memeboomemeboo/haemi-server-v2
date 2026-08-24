package com.memeboo2.haemi.elder.training;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.elder.training.application.TrainingSessionService;
import com.memeboo2.haemi.elder.training.application.TrainingSessionUseCase;
import com.memeboo2.haemi.elder.training.application.TrainingSessionView;
import com.memeboo2.haemi.elder.training.domain.QuestionType;
import com.memeboo2.haemi.elder.training.domain.SessionStatus;
import com.memeboo2.haemi.elder.training.domain.TrainingSession;
import com.memeboo2.haemi.elder.training.infrastructure.TrainingSessionRepository;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrainingSessionUseCaseTest {

    private static final UUID ELDER_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-08-24T01:30:00Z");

    @Mock TrainingSessionRepository repository;
    @Mock HaemiClock clock;
    @Mock CareAccessQuery careAccessQuery;
    @InjectMocks TrainingSessionService service;

    TrainingSessionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = service;
        lenient().when(clock.now()).thenReturn(NOW);
        lenient().when(clock.today()).thenReturn(LocalDate.of(2026, 8, 24));
        lenient().when(careAccessQuery.elderIdForUser(ELDER_ID)).thenReturn(ELDER_ID);
        lenient().when(repository.saveAndFlush(any(TrainingSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void 첫_진입은_새_세션을_만들고_지남력부터_시작한다() {
        TrainingSessionView view = useCase.enter(ELDER_ID);

        assertThat(view.id()).isNotNull();
        assertThat(view.status()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(view.currentStep()).isEqualTo(QuestionType.ORIENTATION);
        assertThat(view.startedAt()).isEqualTo(NOW);
        assertThat(view.completedAt()).isNull();
        verify(careAccessQuery).elderIdForUser(ELDER_ID);
        verify(careAccessQuery).requireSelf(ELDER_ID, ELDER_ID);
        verify(repository).saveAndFlush(any(TrainingSession.class));
    }

    @Test
    void 각_훈련_단계를_순서대로_완료하면_마지막에_완료_결과를_반환한다() {
        useCase.enter(ELDER_ID);
        TrainingSession session = savedSession();
        given(repository.findFirstByElderIdAndStatusOrderByStartedAtAsc(ELDER_ID, SessionStatus.IN_PROGRESS))
                .willReturn(Optional.of(session));

        TrainingSessionView recall = useCase.completeCurrentStep(ELDER_ID, QuestionType.ORIENTATION);
        TrainingSessionView language = useCase.completeCurrentStep(ELDER_ID, QuestionType.RECALL);
        TrainingSessionView delayedRecall = useCase.completeCurrentStep(ELDER_ID, QuestionType.LANGUAGE);
        TrainingSessionView completed = useCase.completeCurrentStep(ELDER_ID, QuestionType.DELAYED_RECALL);

        assertThat(recall.status()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(recall.currentStep()).isEqualTo(QuestionType.RECALL);
        assertThat(language.currentStep()).isEqualTo(QuestionType.LANGUAGE);
        assertThat(delayedRecall.currentStep()).isEqualTo(QuestionType.DELAYED_RECALL);
        assertThat(completed.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(completed.currentStep()).isNull();
        assertThat(completed.completedAt()).isEqualTo(NOW);
        verify(repository, times(5)).saveAndFlush(any(TrainingSession.class));
    }

    @Test
    void 진행_중_재진입은_새_세션을_만들지_않고_저장된_진행_단계를_반환한다() {
        TrainingSessionView firstEntry = useCase.enter(ELDER_ID);
        TrainingSession session = savedSession();
        given(repository.findFirstByElderIdAndStatusOrderByStartedAtAsc(ELDER_ID, SessionStatus.IN_PROGRESS))
                .willReturn(Optional.of(session));
        useCase.completeCurrentStep(ELDER_ID, QuestionType.ORIENTATION);

        TrainingSessionView resumed = useCase.enter(ELDER_ID);

        assertThat(resumed.id()).isEqualTo(firstEntry.id());
        assertThat(resumed.status()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(resumed.currentStep()).isEqualTo(QuestionType.RECALL);
        verify(repository, times(2)).saveAndFlush(any(TrainingSession.class));
    }

    private TrainingSession savedSession() {
        ArgumentCaptor<TrainingSession> captor = ArgumentCaptor.forClass(TrainingSession.class);
        verify(repository).saveAndFlush(captor.capture());
        return captor.getValue();
    }
}
