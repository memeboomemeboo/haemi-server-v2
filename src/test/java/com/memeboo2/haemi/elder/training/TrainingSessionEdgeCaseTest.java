package com.memeboo2.haemi.elder.training;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.elder.training.application.TrainingSessionService;
import com.memeboo2.haemi.elder.training.application.TrainingSessionView;
import com.memeboo2.haemi.elder.training.domain.QuestionType;
import com.memeboo2.haemi.elder.training.domain.SessionStatus;
import com.memeboo2.haemi.elder.training.domain.TrainingSession;
import com.memeboo2.haemi.elder.training.infrastructure.TrainingSessionRepository;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

/** CIST-TRN-001의 완료·순서·동시성 경계 조건을 고정한다. */
@ExtendWith(MockitoExtension.class)
class TrainingSessionEdgeCaseTest {

    private static final UUID ELDER_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-08-24T01:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 24);

    @Mock TrainingSessionRepository repository;
    @Mock HaemiClock clock;
    @Mock CareAccessQuery careAccessQuery;

    private TrainingSessionService service;

    @BeforeEach
    void setUp() {
        service = new TrainingSessionService(repository, clock, careAccessQuery);
        lenient().when(clock.now()).thenReturn(NOW);
        lenient().when(clock.today()).thenReturn(TODAY);
        lenient().when(careAccessQuery.elderIdForUser(ELDER_ID)).thenReturn(ELDER_ID);
        lenient().when(repository.saveAndFlush(any(TrainingSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void 완료한_세션에는_단계를_다시_제출할_수_없고_재저장하지_않는다() {
        TrainingSession session = startSession();
        given(repository.findFirstByElderIdAndStatusOrderByStartedAtAsc(ELDER_ID, SessionStatus.IN_PROGRESS))
                .willReturn(Optional.of(session));
        completeAllSteps();
        clearInvocations(repository);
        given(repository.findFirstByElderIdAndStatusOrderByStartedAtAsc(ELDER_ID, SessionStatus.IN_PROGRESS))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.completeCurrentStep(ELDER_ID, QuestionType.DELAYED_RECALL))
                .isInstanceOf(DomainException.class);

        verify(repository, never()).saveAndFlush(any(TrainingSession.class));
    }

    @Test
    void 현재_단계를_건너뛰어_제출할_수_없다() {
        TrainingSession session = startSession();
        given(repository.findFirstByElderIdAndStatusOrderByStartedAtAsc(ELDER_ID, SessionStatus.IN_PROGRESS))
                .willReturn(Optional.of(session));
        clearInvocations(repository);

        assertThatThrownBy(() -> service.completeCurrentStep(ELDER_ID, QuestionType.RECALL))
                .isInstanceOf(DomainException.class);

        verify(repository, never()).saveAndFlush(any(TrainingSession.class));
    }

    @Test
    void 마지막_지연회상_단계_전에는_세션이_완료되지_않는다() {
        TrainingSession session = startSession();
        given(repository.findFirstByElderIdAndStatusOrderByStartedAtAsc(ELDER_ID, SessionStatus.IN_PROGRESS))
                .willReturn(Optional.of(session));

        TrainingSessionView afterOrientation = service.completeCurrentStep(ELDER_ID, QuestionType.ORIENTATION);

        assertThat(afterOrientation.status()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(afterOrientation.currentStep()).isEqualTo(QuestionType.RECALL);
        assertThat(afterOrientation.completedAt()).isNull();
    }

    @Test
    void 동시_시작으로_하루_세션_유니크_제약이_충돌하면_도메인_예외로_변환한다() {
        doThrow(new DataIntegrityViolationException("uk_training_sessions_elder_date"))
                .when(repository).saveAndFlush(any(TrainingSession.class));

        assertThatThrownBy(() -> service.enter(ELDER_ID))
                .isInstanceOf(DomainException.class);
    }

    private TrainingSession startSession() {
        service.enter(ELDER_ID);
        org.mockito.ArgumentCaptor<TrainingSession> captor = org.mockito.ArgumentCaptor.forClass(TrainingSession.class);
        verify(repository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private void completeAllSteps() {
        service.completeCurrentStep(ELDER_ID, QuestionType.ORIENTATION);
        service.completeCurrentStep(ELDER_ID, QuestionType.RECALL);
        service.completeCurrentStep(ELDER_ID, QuestionType.LANGUAGE);
        service.completeCurrentStep(ELDER_ID, QuestionType.DELAYED_RECALL);
    }
}
