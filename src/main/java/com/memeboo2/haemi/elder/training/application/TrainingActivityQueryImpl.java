package com.memeboo2.haemi.elder.training.application;

import com.memeboo2.haemi.elder.training.domain.SessionStatus;
import com.memeboo2.haemi.elder.training.domain.TrainingAnswer;
import com.memeboo2.haemi.elder.training.domain.TrainingSession;
import com.memeboo2.haemi.elder.training.infrastructure.TrainingAnswerRepository;
import com.memeboo2.haemi.elder.training.infrastructure.TrainingSessionRepository;
import com.memeboo2.haemi.guardian.api.TrainingActivityQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrainingActivityQueryImpl implements TrainingActivityQuery {

    private final TrainingSessionRepository sessionRepository;
    private final TrainingAnswerRepository answerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CompletedSession> completedOn(UUID elderId, LocalDate date) {
        return sessionRepository
                .findByElderIdAndSessionDateAndStatus(elderId, date, SessionStatus.COMPLETED).stream()
                .filter(s -> s.getCompletedAt() != null)
                .map(this::toCompletedSession)
                .toList();
    }

    private CompletedSession toCompletedSession(TrainingSession session) {
        List<TrainingAnswer> answers = answerRepository.findBySessionIdOrderByQuestionNumberAsc(session.getId());
        long graded = answers.stream().filter(answer -> answer.getCorrect() != null).count();
        long correct = answers.stream().filter(answer -> Boolean.TRUE.equals(answer.getCorrect())).count();
        int accuracy = graded == 0 ? 0 : (int) Math.round(correct * 100.0 / graded);
        int durationMinutes = Math.max(1,
                (int) Duration.between(session.getStartedAt(), session.getCompletedAt()).toMinutes());
        return new CompletedSession(session.getCompletedAt(), durationMinutes, accuracy);
    }
}
