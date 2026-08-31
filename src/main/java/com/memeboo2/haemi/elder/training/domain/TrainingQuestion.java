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

import java.time.LocalDate;
import java.time.format.TextStyle;
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

    private static final Locale KOREAN = Locale.KOREAN;

    /** 서술형 부분 일치에 쓰는 정답 키의 최소 길이. 너무 짧은 키는 아무 답이나 통과시키므로 제외한다. (#139) */
    private static final int MIN_DESCRIPTIVE_KEY_LENGTH = 2;

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

    /**
     * 답을 채점한다.
     *
     * @param gradingDate 채점 시각의 KST 날짜. 지남력(날짜/요일/연도) 문항은 세션 시작 시점이 아니라
     *                    이 날짜를 기준으로 정답을 판단한다 — 지남력은 정의상 "지금"을 묻기 때문이다. (#135)
     */
    public Boolean evaluate(String selectedOption, String textAnswer, String voiceMediaKey, LocalDate gradingDate) {
        if (answerMode == AnswerMode.CHOICE) {
            if (selectedOption == null || !options.contains(selectedOption)) {
                throw new DomainException(ErrorCode.INVALID_INPUT, "현재 문항의 보기를 선택해주세요.");
            }
            if (isOrientationTimeQuestion()) {
                // 자정을 넘겨 이어가는 세션에서도 지남력은 채점 시각의 실제 날짜로 평가한다.
                String expected = orientationAnswer(questionKind, gradingDate);
                return normalized(selectedOption).equals(normalized(expected));
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
        // ORIENTATION_YEAR는 채점 시각 기준으로 별도 평가하므로 여기서 제외한다.
        return questionKind == QuestionKind.RECALL_YEAR;
    }

    private boolean isOrientationTimeQuestion() {
        return questionKind == QuestionKind.ORIENTATION_DATE
                || questionKind == QuestionKind.ORIENTATION_WEEKDAY
                || questionKind == QuestionKind.ORIENTATION_YEAR;
    }

    /**
     * 지남력 문항의 정답 문자열을 주어진 날짜로부터 만든다.
     * 문항 생성(보기·정답 키)과 채점이 같은 포맷을 공유해, 표기와 채점 기준이 어긋나지 않게 한다.
     */
    public static String orientationAnswer(QuestionKind kind, LocalDate date) {
        return switch (kind) {
            case ORIENTATION_DATE -> date.getMonthValue() + "월 " + date.getDayOfMonth() + "일";
            case ORIENTATION_WEEKDAY -> date.getDayOfWeek().getDisplayName(TextStyle.FULL, KOREAN);
            case ORIENTATION_YEAR -> String.valueOf(date.getYear());
            default -> throw new IllegalArgumentException("지남력 문항이 아닙니다: " + kind);
        };
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
        String normalizedAnswer = normalized(answer);
        return Arrays.stream(answerKey.split("\\u001F"))
                .map(TrainingQuestion::normalized)
                // 부분 문자열 포함은 어르신 서술형에 관대하게 열어두되, 상한 없는 관대함을 막는다:
                // 한 글자(또는 빈) 키는 "봄"이 "기억이안나봄"까지 정답으로 만들거나 contains("")가 늘 참이 되므로 제외한다. (#139)
                .filter(key -> key.length() >= MIN_DESCRIPTIVE_KEY_LENGTH)
                .anyMatch(normalizedAnswer::contains);
    }

    private static String normalized(String value) {
        return value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }
}
