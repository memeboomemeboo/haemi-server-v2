package com.memeboo2.haemi.guardian.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinFamilyRequest(@NotBlank @Size(max = 12) String inviteCode) {}
