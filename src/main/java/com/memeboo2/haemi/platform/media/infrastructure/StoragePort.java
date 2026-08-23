package com.memeboo2.haemi.platform.media.infrastructure;

import com.memeboo2.haemi.platform.media.domain.MediaType;

import java.net.URI;
import java.util.Optional;

public interface StoragePort {

    /**
     * @return presigned PUT URL (클라이언트가 직접 스토리지에 업로드)
     */
    URI generatePresignedPutUrl(String storageKey, String contentType, long expirySeconds,
                                Integer expectedDurationSeconds);

    /**
     * @return 서빙 URL (확정된 미디어에 접근)
     */
    URI generateServingUrl(String storageKey);

    String buildStorageKey(MediaType mediaType, String originalFilename);

    /** 업로드 확정 시 실제 객체의 메타데이터를 검증한다. */
    default Optional<ObjectMetadata> headObject(String storageKey) {
        return Optional.empty();
    }

    record ObjectMetadata(String contentType, long sizeBytes, Integer durationSeconds) {
        public ObjectMetadata(String contentType, long sizeBytes) {
            this(contentType, sizeBytes, null);
        }
    }
}
