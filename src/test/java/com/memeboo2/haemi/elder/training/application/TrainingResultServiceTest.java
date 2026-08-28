package com.memeboo2.haemi.elder.training.application;

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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TrainingResultServiceTest {

    @Mock TrainingAnswerRepository answerRepository;
    @Mock AttendanceQuery attendanceQuery;
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
}
