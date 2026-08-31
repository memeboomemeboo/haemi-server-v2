package com.memeboo2.haemi.platform.media.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.platform.media.domain.MediaRef;
import com.memeboo2.haemi.platform.media.domain.MediaType;
import com.memeboo2.haemi.platform.media.domain.UploadStatus;
import com.memeboo2.haemi.platform.media.infrastructure.MediaRefRepository;
import com.memeboo2.haemi.platform.media.infrastructure.StoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RequestUploadUseCase {

    private final UploadPolicyProperties policy;
    private final StoragePort storage;
    private final MediaRefRepository repository;
    private final HaemiClock clock;

    @Transactional
    public Result request(UUID uploaderId, MediaType mediaType, String originalFilename, String contentType,
                          long declaredSizeBytes, Integer declaredDurationSeconds) {
        return request(uploaderId, mediaType, originalFilename, contentType,
                declaredSizeBytes, declaredDurationSeconds, null);
    }

    @Transactional
    public Result request(UUID uploaderId, MediaType mediaType, String originalFilename, String contentType,
                          long declaredSizeBytes, Integer declaredDurationSeconds, String contentHash) {
        validate(mediaType, originalFilename, contentType, declaredSizeBytes, declaredDurationSeconds);

        // 해시를 한 번만 정규화한다(소문자, 공백/빈값은 null).
        String normalizedHash = (contentHash == null || contentHash.isBlank()) ? null : contentHash.toLowerCase();

        // SHA-256 중복 방지: 동일 업로더·동일 용도로 이미 확정한 동일 해시 객체만 재사용한다.
        // MediaRef는 용도를 함께 보관하므로, 다른 용도의 참조를 재사용하면 확정 단계에서 용도 검증이 실패한다.
        if (normalizedHash != null) {
            Optional<MediaRef> existing = repository.findFirstByUploaderIdAndMediaTypeAndContentHashAndStatus(
                    uploaderId, mediaType, normalizedHash, UploadStatus.CONFIRMED);
            if (existing.isPresent()) {
                MediaRef reused = existing.get();
                return Result.duplicate(reused.getId(), storage.generateServingUrl(reused.getStorageKey()));
            }
        }

        String storageKey = storage.buildStorageKey(mediaType, originalFilename);
        long expirySeconds = policy.presignedUrl().expiry().toSeconds();
        Instant now = clock.now();
        Instant expiresAt = now.plusSeconds(expirySeconds);
        Instant retainUntil = resolveRetainUntil(mediaType, now);

        URI presignedUrl = storage.generatePresignedPutUrl(
                storageKey, contentType, expirySeconds, declaredDurationSeconds);

        MediaRef ref = MediaRef.pending(mediaType, storageKey, originalFilename, contentType,
                declaredSizeBytes, declaredDurationSeconds, uploaderId, expiresAt, retainUntil,
                normalizedHash);
        repository.save(ref);

        return new Result(ref.getId(), presignedUrl, expiresAt, false, null);
    }

    private void validate(MediaType mediaType, String originalFilename, String contentType, long sizeBytes,
                          Integer declaredDurationSeconds) {
        if (originalFilename == null || originalFilename.isBlank() || originalFilename.length() > 255) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "파일명은 255자 이하여야 합니다.");
        }
        switch (mediaType) {
            case MEMORY_IMAGE, RESPONSE_IMAGE -> {
                if (!policy.image().allowedContentTypes().contains(contentType))
                    throw new DomainException(ErrorCode.INVALID_INPUT);
                if (sizeBytes > policy.image().maxSizeBytes())
                    throw new DomainException(ErrorCode.INVALID_INPUT);
            }
            case RESPONSE_VOICE, GREETING_VOICE -> {
                if (!policy.voice().allowedContentTypes().contains(contentType))
                    throw new DomainException(ErrorCode.INVALID_INPUT);
                if (sizeBytes > policy.voice().maxSizeBytes())
                    throw new DomainException(ErrorCode.INVALID_INPUT);
                if (declaredDurationSeconds == null || declaredDurationSeconds <= 0
                        || declaredDurationSeconds > policy.voice().maxDurationSeconds()) {
                    throw new DomainException(ErrorCode.INVALID_INPUT,
                            "음성은 " + policy.voice().maxDurationSeconds() + "초 이하입니다.");
                }
            }
            case PROFILE_IMAGE -> {
                if (!policy.profile().allowedContentTypes().contains(contentType))
                    throw new DomainException(ErrorCode.INVALID_INPUT);
                if (sizeBytes > policy.profile().maxSizeBytes())
                    throw new DomainException(ErrorCode.INVALID_INPUT);
            }
        }
        if (mediaType != MediaType.RESPONSE_VOICE && mediaType != MediaType.GREETING_VOICE
                && declaredDurationSeconds != null) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "이미지에는 음성 길이를 입력할 수 없습니다.");
        }
    }

    private Instant resolveRetainUntil(MediaType mediaType, Instant now) {
        return switch (mediaType) {
            case MEMORY_IMAGE -> now.plusSeconds(86400L * policy.retention().memoryDays());
            case RESPONSE_IMAGE, RESPONSE_VOICE, GREETING_VOICE -> now.plusSeconds(86400L * policy.retention().responseDays());
            case PROFILE_IMAGE -> null;
        };
    }

    public record Result(UUID mediaRefId, URI presignedUrl, Instant expiresAt, boolean duplicate, URI servingUrl) {
        static Result duplicate(UUID mediaRefId, URI servingUrl) {
            return new Result(mediaRefId, null, null, true, servingUrl);
        }
    }
}
