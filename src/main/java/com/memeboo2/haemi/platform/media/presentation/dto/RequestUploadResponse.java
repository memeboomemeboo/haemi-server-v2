package com.memeboo2.haemi.platform.media.presentation.dto;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record RequestUploadResponse(
        UUID mediaRefId,
        URI presignedUrl,
        Instant expiresAt
) {}
