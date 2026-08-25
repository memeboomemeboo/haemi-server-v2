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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "elder_training_answers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_training_answers_session_number", columnNames = {"session_id", "question_number"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainingAnswer extends BaseEntity {

    @Column(nullable = false)
    private UUID sessionId;

    @Column(nullable = false)
    private UUID questionId;

    @Column(nullable = false)
    private UUID elderId;

    @Column(nullable = false)
    private int questionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionType questionType;

    @Column(length = 100)
    private String selectedOption;

    @Column(length = 500)
    private String textAnswer;

    @Column(length = 500)
    private String voiceMediaKey;

    @Column
    private Boolean correct;

    @Column(nullable = false)
    private Instant answeredAt;

    public static TrainingAnswer record(
            UUID sessionId,
            UUID questionId,
            UUID elderId,
            int questionNumber,
            QuestionType questionType,
            String selectedOption,
            String textAnswer,
            String voiceMediaKey,
            Boolean correct,
            Instant answeredAt
    ) {
        TrainingAnswer answer = new TrainingAnswer();
        answer.assignIdIfAbsent();
        answer.sessionId = sessionId;
        answer.questionId = questionId;
        answer.elderId = elderId;
        answer.questionNumber = questionNumber;
        answer.questionType = questionType;
        answer.selectedOption = selectedOption;
        answer.textAnswer = textAnswer;
        answer.voiceMediaKey = voiceMediaKey;
        answer.correct = correct;
        answer.answeredAt = answeredAt;
        return answer;
    }
}
