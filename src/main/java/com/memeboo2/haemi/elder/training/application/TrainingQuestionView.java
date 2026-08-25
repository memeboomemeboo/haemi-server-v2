package com.memeboo2.haemi.elder.training.application;

import com.memeboo2.haemi.elder.training.domain.AnswerMode;
import com.memeboo2.haemi.elder.training.domain.QuestionType;
import com.memeboo2.haemi.elder.training.domain.TrainingQuestion;

import java.util.List;
import java.util.UUID;

/** 어르신 앱에 내려가는 현재 문항. 정답·정답률은 절대 노출하지 않는다. */
public record TrainingQuestionView(
        UUID id,
        int questionNumber,
        QuestionType questionType,
        AnswerMode answerMode,
        String prompt,
        String imageKey,
        List<String> options,
        String hint
) {
    public static TrainingQuestionView from(TrainingQuestion question) {
        return new TrainingQuestionView(
                question.getId(), question.getQuestionNumber(), question.getQuestionType(), question.getAnswerMode(),
                question.getPrompt(), question.getImageKey(), List.copyOf(question.getOptions()), question.getHint());
    }
}
