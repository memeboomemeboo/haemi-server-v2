package com.memeboo2.haemi.platform.media.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import com.memeboo2.haemi.platform.api.MediaPurpose;
import com.memeboo2.haemi.platform.media.domain.MediaRef;
import com.memeboo2.haemi.platform.media.infrastructure.MediaRefRepository;
import com.memeboo2.haemi.platform.media.infrastructure.StoragePort;
import com.memeboo2.haemi.platform.media.domain.MediaType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConfirmUploadUseCase implements MediaUploadCommand {

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

        ref.confirm(clock.now());

        if (isHeicImage(ref)) {
            convertHeicToJpeg(ref);
        }

        return storage.generateServingUrl(ref.getStorageKey());
    }

    private boolean isVoice(MediaType mediaType) {
        return mediaType == MediaType.RESPONSE_VOICE || mediaType == MediaType.GREETING_VOICE;
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
