package com.memeboo2.haemi.elder.training;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.common.event.TrainingSessionCompleted;
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
import org.springframework.context.ApplicationEventPublisher;

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
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TrainingSessionUseCaseTest {

    private static final UUID ELDER_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-08-24T01:30:00Z");

    @Mock TrainingSessionRepository repository;
    @Mock HaemiClock clock;
    @Mock CareAccessQuery careAccessQuery;
    @Mock ApplicationEventPublisher eventPublisher;
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
        assertThat(view.currentQuestionNumber()).isEqualTo(1);
        assertThat(view.totalQuestionCount()).isEqualTo(10);
        assertThat(view.startedAt()).isEqualTo(NOW);
        assertThat(view.completedAt()).isNull();
        verify(careAccessQuery).elderIdForUser(ELDER_ID);
        verify(careAccessQuery).requireSelf(ELDER_ID, ELDER_ID);
        verify(repository).saveAndFlush(any(TrainingSession.class));
    }

    @Test
    void 열번째_문항을_완료할_때만_세션을_완료하고_이벤트를_발행한다() {
        useCase.enter(ELDER_ID);
        TrainingSession session = savedSession();
        given(repository.findFirstByElderIdAndStatusOrderByStartedAtAsc(ELDER_ID, SessionStatus.IN_PROGRESS))
                .willReturn(Optional.of(session));

        complete(QuestionType.ORIENTATION, 3);
        complete(QuestionType.RECALL, 3);
        complete(QuestionType.LANGUAGE, 2);
        TrainingSessionView ninth = complete(QuestionType.DELAYED_RECALL, 1);

        assertThat(ninth.status()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(ninth.currentStep()).isEqualTo(QuestionType.DELAYED_RECALL);
        assertThat(ninth.currentQuestionNumber()).isEqualTo(10);
        verifyNoInteractions(eventPublisher);

        TrainingSessionView completed = useCase.completeCurrentQuestion(ELDER_ID, QuestionType.DELAYED_RECALL);

        assertThat(completed.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(completed.currentStep()).isNull();
        assertThat(completed.currentQuestionNumber()).isNull();
        assertThat(completed.completedAt()).isEqualTo(NOW);
        verify(repository, times(11)).saveAndFlush(any(TrainingSession.class));
        ArgumentCaptor<TrainingSessionCompleted> eventCaptor = ArgumentCaptor.forClass(TrainingSessionCompleted.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().elderId()).isEqualTo(ELDER_ID);
        assertThat(eventCaptor.getValue().completedDate()).isEqualTo(LocalDate.of(2026, 8, 24));
    }

    @Test
    void 진행_중_재진입은_새_세션을_만들지_않고_저장된_진행_단계를_반환한다() {
        TrainingSessionView firstEntry = useCase.enter(ELDER_ID);
        TrainingSession session = savedSession();
        given(repository.findFirstByElderIdAndStatusOrderByStartedAtAsc(ELDER_ID, SessionStatus.IN_PROGRESS))
                .willReturn(Optional.of(session));
        useCase.completeCurrentQuestion(ELDER_ID, QuestionType.ORIENTATION);

        TrainingSessionView resumed = useCase.enter(ELDER_ID);

        assertThat(resumed.id()).isEqualTo(firstEntry.id());
        assertThat(resumed.status()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(resumed.currentStep()).isEqualTo(QuestionType.ORIENTATION);
        assertThat(resumed.currentQuestionNumber()).isEqualTo(2);
        verify(repository, times(2)).saveAndFlush(any(TrainingSession.class));
    }

    private TrainingSessionView complete(QuestionType questionType, int count) {
        TrainingSessionView result = null;
        for (int i = 0; i < count; i++) {
            result = useCase.completeCurrentQuestion(ELDER_ID, questionType);
        }
        return result;
    }

    private TrainingSession savedSession() {
        ArgumentCaptor<TrainingSession> captor = ArgumentCaptor.forClass(TrainingSession.class);
        verify(repository).saveAndFlush(captor.capture());
        return captor.getValue();
    }
}
