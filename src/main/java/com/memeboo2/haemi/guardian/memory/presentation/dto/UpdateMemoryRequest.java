package com.memeboo2.haemi.guardian.memory.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record UpdateMemoryRequest(
        @NotBlank @Size(max = 100) String title,
        @Size(max = 300) String memo,
        @NotBlank @Size(max = 100) String message,
        Integer memoryYear,
        @Size(max = 4) List<UUID> mediaRefIds
) {
    public UpdateMemoryRequest {
        if (mediaRefIds == null) mediaRefIds = List.of();
    }
}
