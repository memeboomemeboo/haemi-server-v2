package com.memeboo2.haemi.guardian.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateFamilyRequest(
        @NotBlank @Size(max = 50) String name,
        @Size(max = 30) String memo,
        UUID profileImageMediaRefId
) {}
