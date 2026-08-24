package com.memeboo2.haemi.platform.media.presentation.dto;

import com.memeboo2.haemi.platform.media.domain.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RequestUploadRequest(
        @NotNull MediaType mediaType,
        @NotBlank String originalFilename,
        @NotBlank String contentType,
        @Positive long declaredSizeBytes,
        @Positive Integer declaredDurationSeconds
) {}
