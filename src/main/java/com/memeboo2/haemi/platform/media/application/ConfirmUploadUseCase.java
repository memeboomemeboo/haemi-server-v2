package com.memeboo2.haemi.platform.media.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import com.memeboo2.haemi.platform.api.MediaPurpose;
import com.memeboo2.haemi.platform.media.domain.MediaRef;
import com.memeboo2.haemi.platform.media.domain.UploadStatus;
import com.memeboo2.haemi.platform.media.infrastructure.MediaRefRepository;
import com.memeboo2.haemi.platform.media.infrastructure.StoragePort;
import com.memeboo2.haemi.platform.media.domain.MediaType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConfirmUploadUseCase implements MediaUploadCommand {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConfirmUploadUseCase.class);
    private static final java.util.Set<String> HEIC_CONTENT_TYPES = java.util.Set.of("image/heic", "image/heif");

    private final MediaRefRepository repository;
    private final StoragePort storage;
    private final HaemiClock clock;
    private final UploadPolicyProperties policy;
    private final HeicImageConverter heicConverter;

    @Override
    @Transactional(noRollbackFor = DomainException.class)
    public URI confirmUpload(UUID actorId, UUID mediaRefId) {
        return confirmUpload(actorId, mediaRefId, null);
    }

    @Override
    public int memoryImageMaxCount() {
        return policy.image().memoryMaxCount();
    }

    @Override
    @Transactional(readOnly = true)
    public Integer declaredDurationSeconds(UUID mediaRefId) {
        return repository.findById(mediaRefId)
                .map(MediaRef::getDeclaredDurationSeconds)
                .orElse(null);
    }

    @Override
    public Optional<ConfirmedMedia> readConfirmedMedia(UUID mediaRefId, MediaPurpose expectedPurpose) {
        return repository.findById(mediaRefId)
                .filter(ref -> ref.getStatus() == UploadStatus.CONFIRMED)
                .filter(ref -> ref.getMediaType() == MediaType.valueOf(expectedPurpose.name()))
                .flatMap(ref -> storage.getObject(ref.getStorageKey()))
                .map(content -> new ConfirmedMedia(content.contentType(), content.content()));
    }

    @Override
    @Transactional(noRollbackFor = DomainException.class)
    public URI confirmUpload(UUID actorId, UUID mediaRefId, MediaPurpose expectedPurpose) {
        return confirmUpload(actorId, mediaRefId, expectedPurpose, null);
    }

    @Override
    @Transactional(noRollbackFor = DomainException.class)
    public URI confirmUpload(UUID actorId, UUID mediaRefId, MediaPurpose expectedPurpose,
                             Integer expectedDurationSeconds) {
        MediaRef ref = repository.findById(mediaRefId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!ref.isOwnedBy(actorId)) {
            throw new DomainException(ErrorCode.NOT_RESOURCE_OWNER);
        }
        if (expectedPurpose != null && ref.getMediaType() != MediaType.valueOf(expectedPurpose.name())) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "요청한 용도의 미디어가 아닙니다.");
        }
        guardSameHashConfirmation(ref);

        StoragePort.ObjectMetadata metadata = storage.headObject(ref.getStorageKey())
                .orElseThrow(() -> new DomainException(ErrorCode.INVALID_INPUT, "업로드된 파일을 찾을 수 없습니다."));
        if (!ref.getContentType().equals(metadata.contentType())
                || ref.getDeclaredSizeBytes() != metadata.sizeBytes()) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "업로드 파일의 메타데이터가 요청과 다릅니다.");
        }
        if (isVoice(ref.getMediaType())) {
            Integer actualDurationSeconds = metadata.durationSeconds();
            if (actualDurationSeconds == null || !actualDurationSeconds.equals(ref.getDeclaredDurationSeconds())
                    || actualDurationSeconds > policy.voice().maxDurationSeconds()
                    || (expectedDurationSeconds != null && !actualDurationSeconds.equals(expectedDurationSeconds))) {
                throw new DomainException(ErrorCode.INVALID_INPUT, "업로드 음성 길이를 검증할 수 없습니다.");
            }
        }

        // 확정 중 수행하는 서버 측 복사·변환 전에 만료를 확인한다.
        // 실패 시에도 ref를 mutate하기 전에 예외를 던지므로 상태는 PENDING으로 남는다.
        String originalKeyToPurge = null;
        if (ref.getStatus() != UploadStatus.CONFIRMED) {
            java.time.Instant now = clock.now();
            ref.ensureConfirmable(now);
            if (isHeicImage(ref)) {
                originalKeyToPurge = ref.getStorageKey();
                convertHeicToJpeg(ref);
            } else {
                // 확정 객체는 presigned PUT URL이 가리키는 임시 키와 분리한다. URL이 아직 유효해도
                // 확정 후 서빙·전사 대상의 바이트가 덮어써지지 않는다.
                originalKeyToPurge = ref.getStorageKey();
                String confirmedKey = storage.buildStorageKey(ref.getMediaType(), ref.getOriginalFilename());
                storage.copyObject(originalKeyToPurge, confirmedKey, metadata.eTag());
                ref.replaceStorage(confirmedKey, ref.getContentType(), ref.getDeclaredSizeBytes());
            }
            ref.confirm(now);
        }

        // confirm 성공 후에만 확정 전 임시 객체를 정리한다(고아 객체 방지). 실패해도 서빙에는 영향 없음.
        if (originalKeyToPurge != null) {
            try {
                storage.deleteObject(originalKeyToPurge);
            } catch (RuntimeException e) {
                // best-effort 정리 — 실패는 로깅만 하고 확정 결과를 막지 않는다.
                log.warn("확정 전 임시 객체 정리 실패(고아 객체 가능): key={}, cause={}", originalKeyToPurge, e.toString());
            }
        }

        return storage.generateServingUrl(ref.getStorageKey());
    }

    private boolean isVoice(MediaType mediaType) {
        return mediaType == MediaType.RESPONSE_VOICE || mediaType == MediaType.GREETING_VOICE;
    }

    private void guardSameHashConfirmation(MediaRef ref) {
        if (ref.getContentHash() == null) {
            return;
        }

        // 두 요청이 모두 PENDING일 때도 같은 첫 행을 잠가 확인 순서를 직렬화한다.
        repository.findFirstByUploaderIdAndMediaTypeAndContentHashOrderByIdAsc(
                ref.getUploaderId(), ref.getMediaType(), ref.getContentHash());
        repository.findFirstByUploaderIdAndMediaTypeAndContentHashAndStatus(
                        ref.getUploaderId(), ref.getMediaType(), ref.getContentHash(), UploadStatus.CONFIRMED)
                .filter(existing -> existing != ref)
                .ifPresent(existing -> {
                    throw new DomainException(ErrorCode.MEDIA_DUPLICATE_ALREADY_CONFIRMED);
                });
    }

    private boolean isHeicImage(MediaRef ref) {
        boolean image = ref.getMediaType() == MediaType.MEMORY_IMAGE
                || ref.getMediaType() == MediaType.RESPONSE_IMAGE
                || ref.getMediaType() == MediaType.PROFILE_IMAGE;
        return image && HEIC_CONTENT_TYPES.contains(ref.getContentType().toLowerCase());
    }

    /** confirm 시 동기 변환: HEIC 원본을 읽어 JPEG로 변환·재저장하고 MediaRef를 갱신한다. */
    private void convertHeicToJpeg(MediaRef ref) {
        StoragePort.StoredContent original = storage.getObject(ref.getStorageKey())
                .orElseThrow(() -> new DomainException(ErrorCode.MEDIA_CONVERSION_FAILED,
                        "변환할 원본 이미지를 찾을 수 없습니다."));

        byte[] jpeg = heicConverter.toJpeg(original.content());
        String jpegKey = toJpegKey(ref.getStorageKey());
        storage.putObject(jpegKey, "image/jpeg", jpeg);
        ref.replaceStorage(jpegKey, "image/jpeg", jpeg.length);
    }

    private String toJpegKey(String storageKey) {
        int dot = storageKey.lastIndexOf('.');
        int slash = storageKey.lastIndexOf('/');
        if (dot > slash) {
            return storageKey.substring(0, dot) + ".jpg";
        }
        return storageKey + ".jpg";
    }
}
