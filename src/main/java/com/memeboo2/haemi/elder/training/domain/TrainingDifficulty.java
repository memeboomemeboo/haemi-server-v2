package com.memeboo2.haemi.elder.training.domain;

import com.memeboo2.haemi.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "elder_training_difficulties", uniqueConstraints = {
        @UniqueConstraint(name = "uk_training_difficulty_elder_type", columnNames = {"elder_id", "question_type"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainingDifficulty extends BaseEntity {

    @Column(nullable = false)
    private UUID elderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionType questionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DifficultyLevel level;

    @Column(nullable = false)
    private int consecutiveHighDays;

    @Column
    private LocalDate lastEvaluatedDate;

    public static TrainingDifficulty start(UUID elderId, QuestionType questionType) {
        TrainingDifficulty difficulty = new TrainingDifficulty();
        difficulty.assignIdIfAbsent();
        difficulty.elderId = elderId;
        difficulty.questionType = questionType;
        difficulty.level = DifficultyLevel.LEVEL_1;
        return difficulty;
    }

    public void evaluate(LocalDate date, double accuracy, double promotionAccuracy,
                         double demotionAccuracy, int promotionDays) {
        if (accuracy <= demotionAccuracy) {
            level = level.lower();
            consecutiveHighDays = 0;
            lastEvaluatedDate = date;
            return;
        }

        if (accuracy >= promotionAccuracy) {
            consecutiveHighDays = lastEvaluatedDate != null && lastEvaluatedDate.plusDays(1).equals(date)
                    ? consecutiveHighDays + 1
                    : 1;
            if (consecutiveHighDays >= promotionDays) {
                level = level.raise();
                consecutiveHighDays = 0;
            }
        } else {
            consecutiveHighDays = 0;
        }
        lastEvaluatedDate = date;
    }
}
