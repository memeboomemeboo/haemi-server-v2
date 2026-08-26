package com.memeboo2.haemi.platform.media.presentation.dto;

import com.memeboo2.haemi.platform.media.domain.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record RequestUploadRequest(
        @NotNull MediaType mediaType,
        @NotBlank String originalFilename,
        @NotBlank String contentType,
        @Positive long declaredSizeBytes,
        @Positive Integer declaredDurationSeconds,
        @Schema(description = "클라이언트가 계산한 파일의 SHA-256(hex 64자). 제공 시 동일 업로더 중복 업로드를 방지한다.",
                example = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
        @Pattern(regexp = "^[A-Fa-f0-9]{64}$", message = "contentHash는 SHA-256 hex 64자여야 합니다.")
        String contentHash
) {}
