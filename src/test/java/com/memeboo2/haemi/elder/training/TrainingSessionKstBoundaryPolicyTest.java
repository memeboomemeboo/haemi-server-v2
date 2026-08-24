package com.memeboo2.haemi.elder.training;

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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class TrainingSessionKstBoundaryPolicyTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final UUID ELDER_ID = UUID.randomUUID();
    private static final LocalDate AUGUST_25 = LocalDate.of(2026, 8, 25);
    private static final Instant AUGUST_25_START = AUGUST_25.atStartOfDay(KST).toInstant();
    private static final Instant AUGUST_26_START = AUGUST_25.plusDays(1).atStartOfDay(KST).toInstant();

    @Mock TrainingSessionRepository trainingSessionRepository;
    @Mock HaemiClock clock;
    @Mock CareAccessQuery careAccessQuery;
    @Mock ApplicationEventPublisher eventPublisher;

    private TrainingSessionService service;

    @BeforeEach
    void setUp() {
        service = new TrainingSessionService(trainingSessionRepository, clock, careAccessQuery, eventPublisher);
        given(careAccessQuery.elderIdForUser(ELDER_ID)).willReturn(ELDER_ID);
    }

    @Test
    void 자정이_지나도_미완료_세션은_이어하기로_반환한다() {
        TrainingSession inProgress = 세션(SessionStatus.IN_PROGRESS, QuestionType.RECALL,
                Instant.parse("2026-08-24T14:55:00Z"), null); // 8/24 23:55 KST
        given(trainingSessionRepository.findFirstByElderIdAndStatusOrderByStartedAtAsc(
                ELDER_ID, SessionStatus.IN_PROGRESS)).willReturn(Optional.of(inProgress));

        TrainingSessionView result = service.enter(ELDER_ID);

        assertThat(result.id()).isEqualTo(inProgress.getId());
        assertThat(result.status()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(result.currentStep()).isEqualTo(QuestionType.RECALL);
        then(trainingSessionRepository).should(never())
                .findFirstByElderIdAndStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
                        any(), any(), any(), any());
        then(trainingSessionRepository).should(never()).saveAndFlush(any());
    }

    @Test
    void 자정_이후_완료하면_완료일에는_새_세션을_생성하지_않는다() {
        TrainingSession completed = 세션(SessionStatus.COMPLETED, QuestionType.DELAYED_RECALL,
                Instant.parse("2026-08-24T14:55:00Z"), Instant.parse("2026-08-24T15:05:00Z"));
        // completedAt은 8/25 00:05 KST이므로 8/25의 1회 훈련으로 본다.
        given(clock.today()).willReturn(AUGUST_25);
        given(trainingSessionRepository.findFirstByElderIdAndStatusOrderByStartedAtAsc(
                ELDER_ID, SessionStatus.IN_PROGRESS)).willReturn(Optional.empty());
        given(trainingSessionRepository.findFirstByElderIdAndStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
                ELDER_ID, SessionStatus.COMPLETED, AUGUST_25_START, AUGUST_26_START))
                .willReturn(Optional.of(completed));

        TrainingSessionView result = service.enter(ELDER_ID);

        assertThat(result.id()).isEqualTo(completed.getId());
        assertThat(result.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(result.completedAt()).isEqualTo(Instant.parse("2026-08-24T15:05:00Z"));
        then(trainingSessionRepository).should(never()).saveAndFlush(any());
    }

    @Test
    void 완료일의_다음_KST_날짜부터_새_세션을_생성한다() {
        LocalDate august26 = AUGUST_25.plusDays(1);
        Instant august27Start = august26.plusDays(1).atStartOfDay(KST).toInstant();
        Instant now = Instant.parse("2026-08-25T15:00:01Z"); // 8/26 00:00:01 KST
        given(clock.today()).willReturn(august26);
        given(clock.now()).willReturn(now);
        given(trainingSessionRepository.findFirstByElderIdAndStatusOrderByStartedAtAsc(
                ELDER_ID, SessionStatus.IN_PROGRESS)).willReturn(Optional.empty());
        given(trainingSessionRepository.findFirstByElderIdAndStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
                ELDER_ID, SessionStatus.COMPLETED, AUGUST_26_START, august27Start))
                .willReturn(Optional.empty());
        given(trainingSessionRepository.saveAndFlush(any(TrainingSession.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        TrainingSessionView result = service.enter(ELDER_ID);

        assertThat(result.status()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(result.currentStep()).isEqualTo(QuestionType.ORIENTATION);
        assertThat(result.startedAt()).isEqualTo(now);
        assertThat(result.completedAt()).isNull();
        ArgumentCaptor<TrainingSession> sessionCaptor = ArgumentCaptor.forClass(TrainingSession.class);
        then(trainingSessionRepository).should().saveAndFlush(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getElderId()).isEqualTo(ELDER_ID);
    }

    private TrainingSession 세션(SessionStatus status, QuestionType currentStep,
                              Instant startedAt, Instant completedAt) {
        TrainingSession session = org.mockito.Mockito.mock(TrainingSession.class);
        given(session.getId()).willReturn(UUID.randomUUID());
        given(session.getStatus()).willReturn(status);
        given(session.getCurrentStep()).willReturn(currentStep);
        given(session.getStartedAt()).willReturn(startedAt);
        given(session.getCompletedAt()).willReturn(completedAt);
        return session;
    }
}
