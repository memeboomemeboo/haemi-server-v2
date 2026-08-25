package com.memeboo2.haemi.elder.training.application;

import com.memeboo2.haemi.elder.training.domain.QuestionType;
import com.memeboo2.haemi.elder.training.domain.SessionStatus;
import com.memeboo2.haemi.elder.training.domain.TrainingAnswer;
import com.memeboo2.haemi.elder.training.domain.TrainingSession;
import com.memeboo2.haemi.elder.training.infrastructure.TrainingAnswerRepository;
import com.memeboo2.haemi.guardian.api.AttendanceQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainingResultService {

    private final TrainingAnswerRepository answerRepository;
    private final AttendanceQuery attendanceQuery;

    @Transactional(readOnly = true)
    public TrainingResultView resultFor(TrainingSession session) {
        List<TrainingAnswer> answers = answerRepository.findBySessionIdOrderByQuestionNumberAsc(session.getId());
        return new TrainingResultView(
                session.getId(),
                session.getStatus() == SessionStatus.COMPLETED,
                session.getCompletedAt() == null ? 0 : Duration.between(session.getStartedAt(), session.getCompletedAt()).toSeconds(),
                (int) answers.stream()
                        .filter(answer -> answer.getQuestionType() == QuestionType.DELAYED_RECALL)
                        .filter(answer -> Boolean.TRUE.equals(answer.getCorrect()))
                        .count(),
                session.getCompletedAt(),
                session.getStatus() == SessionStatus.COMPLETED
                        ? attendanceQuery.unlockedBadgesAfterCompletion(session.getElderId())
                        : attendanceQuery.unlockedBadges(session.getElderId()));
    }

}
