package com.memeboo2.haemi.guardian.dailycare.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendTextRequest(
        @NotBlank @Size(max = 100) String text
) {}
