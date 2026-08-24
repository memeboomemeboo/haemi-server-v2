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

    private final MediaRefRepository repository;
    private final StoragePort storage;
    private final HaemiClock clock;
    private final UploadPolicyProperties policy;

    @Override
    @Transactional(noRollbackFor = DomainException.class)
    public URI confirmUpload(UUID actorId, UUID mediaRefId) {
        return confirmUpload(actorId, mediaRefId, null);
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

        return storage.generateServingUrl(ref.getStorageKey());
    }

    private boolean isVoice(MediaType mediaType) {
        return mediaType == MediaType.RESPONSE_VOICE || mediaType == MediaType.GREETING_VOICE;
    }
}
