package com.memeboo2.haemi.elder.training.application;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.elder.training.domain.QuestionType;
import com.memeboo2.haemi.elder.training.domain.TrainingAnswer;
import com.memeboo2.haemi.elder.training.domain.TrainingDifficulty;
import com.memeboo2.haemi.elder.training.domain.TrainingSession;
import com.memeboo2.haemi.elder.training.infrastructure.TrainingAnswerRepository;
import com.memeboo2.haemi.elder.training.infrastructure.TrainingDifficultyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 완료 세션 직후 영역별 다음 세션 난이도를 갱신한다. */
@Service
@RequiredArgsConstructor
public class TrainingDifficultyService {

    private final TrainingAnswerRepository answerRepository;
    private final TrainingDifficultyRepository difficultyRepository;
    private final TrainingPolicyProperties policy;

    @Transactional
    public void evaluateCompletedSession(TrainingSession session) {
        for (QuestionType type : QuestionType.values()) {
            List<TrainingAnswer> graded = answerRepository.findBySessionIdAndQuestionType(session.getId(), type).stream()
                    .filter(answer -> answer.getCorrect() != null)
                    .toList();
            if (graded.isEmpty()) {
                continue;
            }
            double accuracy = graded.stream().filter(answer -> answer.getCorrect()).count() / (double) graded.size();
            TrainingDifficulty difficulty = difficultyRepository.findByElderIdAndQuestionType(session.getElderId(), type)
                    .orElseGet(() -> TrainingDifficulty.start(session.getElderId(), type));
            difficulty.evaluate(
                    HaemiClock.dateInKst(session.getCompletedAt()),
                    accuracy,
                    policy.promotionAccuracy(),
                    policy.demotionAccuracy(),
                    policy.promotionConsecutiveDays());
            difficultyRepository.save(difficulty);
        }
    }
}
