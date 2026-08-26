package com.memeboo2.haemi.platform.media.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record RequestUploadResponse(
        UUID mediaRefId,
        @Schema(description = "presigned PUT URL. 중복(duplicate=true)이면 null — 업로드·확정 없이 재사용한다.")
        URI presignedUrl,
        Instant expiresAt,
        @Schema(description = "동일 업로더의 동일 SHA-256 이미지가 이미 존재해 재사용됨")
        boolean duplicate,
        @Schema(description = "중복 시 기존 객체의 서빙 URL (duplicate=true일 때만 채워짐)")
        URI servingUrl
) {}
