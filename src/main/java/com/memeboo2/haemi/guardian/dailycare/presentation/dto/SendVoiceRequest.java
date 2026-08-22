package com.memeboo2.haemi.guardian.dailycare.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record SendVoiceRequest(
        @NotBlank String mediaKey,
        @Positive int durationSeconds
) {}
