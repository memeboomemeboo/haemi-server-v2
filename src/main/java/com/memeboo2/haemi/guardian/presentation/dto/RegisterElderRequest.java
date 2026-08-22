package com.memeboo2.haemi.guardian.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record RegisterElderRequest(
        @NotNull UUID elderUserId,
        @NotNull UUID familyId,
        @NotBlank @Size(max = 30) String name,
        LocalDate birthDate
) {}
