package com.memeboo2.haemi.guardian.memory.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record UpdateMemoryRequest(
        @NotBlank @Size(max = 100) String title,
        @Size(max = 300) String memo,
        @NotBlank @Size(max = 100) String message,
        Integer memoryYear,
        @Min(1) @Max(12) Integer memoryMonth,
        @Size(max = 50) String place,
        @Size(max = 4) List<UUID> mediaRefIds
) {
    public UpdateMemoryRequest {
        if (mediaRefIds == null) mediaRefIds = List.of();
    }
}
