package com.memeboo2.haemi.guardian.dailycare.presentation.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SendVoiceRequest(
        @NotNull UUID mediaRefId,
        @Positive int durationSeconds
) {}
