package com.memeboo2.haemi.elder.training.application;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.elder.training.domain.QuestionType;
import com.memeboo2.haemi.elder.training.domain.SessionStatus;
import com.memeboo2.haemi.elder.training.domain.TrainingAnswer;
import com.memeboo2.haemi.elder.training.domain.TrainingSession;
import com.memeboo2.haemi.elder.training.infrastructure.TrainingAnswerRepository;
import com.memeboo2.haemi.guardian.api.AttendanceQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class TrainingResultServiceTest {

    @Mock TrainingAnswerRepository answerRepository;
    @Mock AttendanceQuery attendanceQuery;
    @Mock HaemiClock clock;
    @InjectMocks TrainingResultService service;

    @Test
    void 완료된_세션은_소요시간과_완료후_배지를_반환한다() {
        UUID sessionId = UUID.randomUUID();
        UUID elderId = UUID.randomUUID();
        TrainingSession session = org.mockito.Mockito.mock(TrainingSession.class);
        given(session.getId()).willReturn(sessionId);
        given(session.getElderId()).willReturn(elderId);
        given(session.getStatus()).willReturn(SessionStatus.COMPLETED);
        given(session.getStartedAt()).willReturn(Instant.parse("2026-08-27T00:00:00Z"));
        given(session.getCompletedAt()).willReturn(Instant.parse("2026-08-27T00:03:00Z"));

        TrainingAnswer correct = org.mockito.Mockito.mock(TrainingAnswer.class);
        given(correct.getQuestionType()).willReturn(QuestionType.DELAYED_RECALL);
        given(correct.getCorrect()).willReturn(true);
        given(answerRepository.findBySessionIdOrderByQuestionNumberAsc(sessionId))
                .willReturn(List.of(correct));
        given(clock.today()).willReturn(LocalDate.of(2026, 8, 27));
        given(attendanceQuery.unlockedBadgesAfterCompletion(elderId)).willReturn(List.of());

        TrainingResultView view = service.resultFor(session);

        assertThat(view.completed()).isTrue();
        assertThat(view.participationSeconds()).isEqualTo(180);
        assertThat(view.delayedRecallSuccessCount()).isEqualTo(1);
    }

    @Test
    void 미완료_세션은_소요시간0과_현재_배지를_반환한다() {
        UUID sessionId = UUID.randomUUID();
        UUID elderId = UUID.randomUUID();
        TrainingSession session = org.mockito.Mockito.mock(TrainingSession.class);
        given(session.getId()).willReturn(sessionId);
        given(session.getElderId()).willReturn(elderId);
        given(session.getStatus()).willReturn(SessionStatus.IN_PROGRESS);
        given(session.getCompletedAt()).willReturn(null);
        given(answerRepository.findBySessionIdOrderByQuestionNumberAsc(sessionId))
                .willReturn(List.of());
        given(attendanceQuery.unlockedBadges(elderId)).willReturn(List.of());

        TrainingResultView view = service.resultFor(session);

        assertThat(view.completed()).isFalse();
        assertThat(view.participationSeconds()).isEqualTo(0);
        assertThat(view.delayedRecallSuccessCount()).isEqualTo(0);
    }

    @Test
    void 어제_완료한_세션을_오늘_다시_열면_완료후_집계를_쓰지_않는다() {
        UUID sessionId = UUID.randomUUID();
        UUID elderId = UUID.randomUUID();
        TrainingSession session = org.mockito.Mockito.mock(TrainingSession.class);
        given(session.getId()).willReturn(sessionId);
        given(session.getElderId()).willReturn(elderId);
        given(session.getStatus()).willReturn(SessionStatus.COMPLETED);
        given(session.getStartedAt()).willReturn(Instant.parse("2026-08-26T00:00:00Z"));
        given(session.getCompletedAt()).willReturn(Instant.parse("2026-08-26T00:03:00Z"));
        given(answerRepository.findBySessionIdOrderByQuestionNumberAsc(sessionId)).willReturn(List.of());
        // 오늘은 8/27 — 완료일(8/26)과 다르므로 '오늘을 참여일로 +1' 하는 경로를 타면 안 된다. (#160)
        given(clock.today()).willReturn(LocalDate.of(2026, 8, 27));
        given(attendanceQuery.unlockedBadges(elderId)).willReturn(List.of());

        service.resultFor(session);

        then(attendanceQuery).should().unlockedBadges(elderId);
        then(attendanceQuery).should(never()).unlockedBadgesAfterCompletion(elderId);
    }

    @Test
    void 완료_시각이_KST로_오늘이면_완료후_집계를_쓴다() {
        UUID sessionId = UUID.randomUUID();
        UUID elderId = UUID.randomUUID();
        TrainingSession session = org.mockito.Mockito.mock(TrainingSession.class);
        given(session.getId()).willReturn(sessionId);
        given(session.getElderId()).willReturn(elderId);
        given(session.getStatus()).willReturn(SessionStatus.COMPLETED);
        given(session.getStartedAt()).willReturn(Instant.parse("2026-08-26T14:50:00Z"));
        // UTC로는 8/26이지만 KST로는 8/27 — 날짜 판정은 KST 기준이어야 한다.
        given(session.getCompletedAt()).willReturn(Instant.parse("2026-08-26T15:10:00Z"));
        given(answerRepository.findBySessionIdOrderByQuestionNumberAsc(sessionId)).willReturn(List.of());
        given(clock.today()).willReturn(LocalDate.of(2026, 8, 27));
        given(attendanceQuery.unlockedBadgesAfterCompletion(elderId)).willReturn(List.of());

        service.resultFor(session);

        then(attendanceQuery).should().unlockedBadgesAfterCompletion(elderId);
        then(attendanceQuery).should(never()).unlockedBadges(elderId);
    }
}
