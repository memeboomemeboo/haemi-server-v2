package com.memeboo2.haemi.guardian.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record RegisterElderRequest(
        @NotNull UUID familyId,
        @NotBlank @Size(max = 30) String name,
        LocalDate birthDate,
        @NotBlank @Size(min = 4, max = 50) String loginId,
        @NotBlank @Pattern(regexp = "\\d{6}") String pin,
        @Size(min = 8, max = 50) String password,
        @NotBlank @Size(max = 20) String phone,
        @NotBlank @Size(max = 20) String gender
) {}
