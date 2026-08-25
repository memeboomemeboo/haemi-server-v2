package com.memeboo2.haemi.elder.training.presentation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CompleteTrainingQuestionRequest(
        @NotNull UUID sessionId,
        @NotNull UUID questionId,
        @Min(1) int questionNumber,
        @Size(max = 200) String selectedOption,
        @Size(max = 1_000) String textAnswer,
        UUID voiceMediaRefId
) {}
