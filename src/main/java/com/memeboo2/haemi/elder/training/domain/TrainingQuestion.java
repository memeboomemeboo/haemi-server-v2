package com.memeboo2.haemi.elder.training.domain;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.persistence.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

@Entity
@Table(name = "elder_training_questions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_training_questions_session_number", columnNames = {"session_id", "question_number"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainingQuestion extends BaseEntity {

    @Column(nullable = false)
    private UUID sessionId;

    @Column(nullable = false)
    private int questionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionType questionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuestionKind questionKind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnswerMode answerMode;

    @Column(nullable = false, length = 300)
    private String prompt;

    @Column(length = 500)
    private String imageKey;

    @Column
    private UUID materialId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MaterialSource materialSource;

    @Column(length = 100)
    private String materialTitle;

    /** 클라이언트에 내려가지 않는 채점 기준이다. */
    @Column(nullable = false, length = 700)
    private String answerKey;

    @Column(nullable = false)
    private int yearTolerance;

    @Column(length = 200)
    private String hint;

    @ElementCollection
    @CollectionTable(name = "elder_training_question_options", joinColumns = @JoinColumn(name = "question_id"))
    @OrderColumn(name = "option_order")
    @Column(name = "option_text", nullable = false, length = 100)
    private List<String> options = new ArrayList<>();

    public static TrainingQuestion choice(
            UUID sessionId,
            int questionNumber,
            QuestionType questionType,
            QuestionKind questionKind,
            String prompt,
            String imageKey,
            TrainingMaterial material,
            String answerKey,
            int yearTolerance,
            String hint,
            List<String> options
    ) {
        TrainingQuestion question = base(
                sessionId, questionNumber, questionType, questionKind, AnswerMode.CHOICE,
                prompt, imageKey, material, answerKey, yearTolerance, hint);
        question.options.addAll(shuffledOptions(sessionId, questionNumber, options));
        return question;
    }

    public static TrainingQuestion textOrVoice(
            UUID sessionId,
            int questionNumber,
            QuestionType questionType,
            QuestionKind questionKind,
            String prompt,
            String imageKey,
            TrainingMaterial material,
            String answerKey,
            String hint
    ) {
        return base(
                sessionId, questionNumber, questionType, questionKind, AnswerMode.TEXT_OR_VOICE,
                prompt, imageKey, material, answerKey, 0, hint);
    }

    public Boolean evaluate(String selectedOption, String textAnswer, String voiceMediaKey) {
        if (answerMode == AnswerMode.CHOICE) {
            if (selectedOption == null || !options.contains(selectedOption)) {
                throw new DomainException(ErrorCode.INVALID_INPUT, "현재 문항의 보기를 선택해주세요.");
            }
            if (isYearAnswer()) {
                try {
                    return Math.abs(Integer.parseInt(selectedOption) - Integer.parseInt(answerKey)) <= yearTolerance;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
            return matchesChoiceAnswer(selectedOption);
        }

        if ((textAnswer == null || textAnswer.isBlank()) && (voiceMediaKey == null || voiceMediaKey.isBlank())) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "텍스트 또는 음성으로 답해주세요.");
        }
        if (voiceMediaKey != null && !voiceMediaKey.isBlank()) {
            return null;
        }
        if (questionType == QuestionType.LANGUAGE) {
            return null;
        }
        return matchesAnswerKey(textAnswer);
    }

    private static TrainingQuestion base(
            UUID sessionId,
            int questionNumber,
            QuestionType questionType,
            QuestionKind questionKind,
            AnswerMode answerMode,
            String prompt,
            String imageKey,
            TrainingMaterial material,
            String answerKey,
            int yearTolerance,
            String hint
    ) {
        TrainingQuestion question = new TrainingQuestion();
        question.assignIdIfAbsent();
        question.sessionId = sessionId;
        question.questionNumber = questionNumber;
        question.questionType = questionType;
        question.questionKind = questionKind;
        question.answerMode = answerMode;
        question.prompt = prompt;
        question.imageKey = imageKey;
        question.materialId = material == null ? null : material.id();
        question.materialSource = material == null ? null : material.source();
        question.materialTitle = material == null ? null : material.title();
        question.answerKey = answerKey;
        question.yearTolerance = yearTolerance;
        question.hint = hint;
        return question;
    }

    private boolean isYearAnswer() {
        return questionKind == QuestionKind.RECALL_YEAR || questionKind == QuestionKind.ORIENTATION_YEAR;
    }

    /** 세션·문항 번호로 고정한 보기 순서라 재진입 때도 동일하며 위치 편향은 피한다. */
    private static List<String> shuffledOptions(UUID sessionId, int questionNumber, List<String> options) {
        List<String> shuffled = new ArrayList<>(options);
        long seed = sessionId.getMostSignificantBits()
                ^ Long.rotateLeft(sessionId.getLeastSignificantBits(), 17)
                ^ questionNumber;
        Collections.shuffle(shuffled, new Random(seed));
        return shuffled;
    }

    private boolean matchesChoiceAnswer(String answer) {
        String normalizedAnswer = normalized(answer);
        return Arrays.stream(answerKey.split("\\u001F"))
                .map(TrainingQuestion::normalized)
                .anyMatch(normalizedAnswer::equals);
    }

    private boolean matchesAnswerKey(String answer) {
        return Arrays.stream(answerKey.split("\\u001F"))
                .map(TrainingQuestion::normalized)
                .anyMatch(key -> normalized(answer).contains(key));
    }

    private static String normalized(String value) {
        return value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }
}
