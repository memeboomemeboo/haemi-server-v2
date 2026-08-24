package com.memeboo2.haemi.elder.training.presentation.dto;

import com.memeboo2.haemi.elder.training.domain.QuestionType;
import jakarta.validation.constraints.NotNull;

public record CompleteTrainingQuestionRequest(@NotNull QuestionType questionType) {}
