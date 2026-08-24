package com.memeboo2.haemi.guardian.presentation.dto;

import com.memeboo2.haemi.guardian.family.application.CreateFamilyUseCase;

import java.util.UUID;

public record CreateFamilyResponse(UUID familyId, String inviteCode) {
    public static CreateFamilyResponse from(CreateFamilyUseCase.Result result) {
        return new CreateFamilyResponse(result.familyId(), result.inviteCode());
    }
}
