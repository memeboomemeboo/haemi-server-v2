package com.memeboo2.haemi.guardian.presentation.dto;

import com.memeboo2.haemi.guardian.api.GuardianRole;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull GuardianRole role) {}
