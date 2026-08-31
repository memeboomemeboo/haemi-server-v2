package com.memeboo2.haemi.elder.training.application;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.elder.training.domain.QuestionType;
import com.memeboo2.haemi.elder.training.domain.SessionStatus;
import com.memeboo2.haemi.elder.training.domain.TrainingAnswer;
import com.memeboo2.haemi.elder.training.domain.TrainingSession;
import com.memeboo2.haemi.elder.training.infrastructure.TrainingAnswerRepository;
import com.memeboo2.haemi.guardian.api.AttendanceBadge;
import com.memeboo2.haemi.guardian.api.AttendanceQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainingResultService {

    private final TrainingAnswerRepository answerRepository;
    private final AttendanceQuery attendanceQuery;
    private final HaemiClock clock;

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
                badgesFor(session));
    }

    /**
     * 결과 화면의 배지 목록을 고른다.
     *
     * <p>{@code unlockedBadgesAfterCompletion}은 '오늘이 참여일'임을 전제로 오늘을 항상 1로 더한다.
     * 결과 화면은 언제든 다시 열람할 수 있으므로, 완료 상태만 보고 이 경로를 타면 어제 완료한 세션을
     * 오늘 다시 열었을 때 오늘을 참여일로 세어 미달성 배지가 노출된다. 완료 시각의 KST 날짜가
     * 오늘과 같을 때 — 즉 전제가 실제로 성립할 때 — 만 완료 직후 집계를 쓴다. (#160)
     */
    private List<AttendanceBadge> badgesFor(TrainingSession session) {
        if (session.getStatus() == SessionStatus.COMPLETED && completedToday(session)) {
            return attendanceQuery.unlockedBadgesAfterCompletion(session.getElderId());
        }
        return attendanceQuery.unlockedBadges(session.getElderId());
    }

    private boolean completedToday(TrainingSession session) {
        Instant completedAt = session.getCompletedAt();
        return completedAt != null && HaemiClock.dateInKst(completedAt).equals(clock.today());
    }

}
