package com.memeboo2.haemi.platform.media.infrastructure;

import com.memeboo2.haemi.platform.media.domain.MediaType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 개발·테스트용 스토리지 스텁. S3 어댑터가 등록되면 대체된다.
 * presigned URL 대신 로컬 업로드 엔드포인트 URL을 반환한다.
 */
@Component
@ConditionalOnMissingBean(name = "s3StorageAdapter")
class LocalStorageAdapter implements StoragePort {

    private final LocalObjectStorage objectStorage;

    LocalStorageAdapter(LocalObjectStorage objectStorage) {
        this.objectStorage = objectStorage;
    }

    @Override
    public URI generatePresignedPutUrl(String storageKey, String contentType, long expirySeconds,
                                       Integer expectedDurationSeconds) {
        String durationQuery = expectedDurationSeconds == null ? "" : "&durationSeconds=" + expectedDurationSeconds;
        return URI.create("http://localhost:8080/internal/storage/upload?key="
                + URLEncoder.encode(storageKey, StandardCharsets.UTF_8)
                + "&contentType=" + URLEncoder.encode(contentType, StandardCharsets.UTF_8)
                + durationQuery);
    }

    @Override
    public URI generateServingUrl(String storageKey) {
        return URI.create("http://localhost:8080/internal/storage/serve?key="
                + URLEncoder.encode(storageKey, StandardCharsets.UTF_8));
    }

    @Override
    public String buildStorageKey(MediaType mediaType, String originalFilename) {
        String ext = extractExtension(originalFilename);
        return mediaType.name().toLowerCase() + "/" + UUID.randomUUID() + ext;
    }

    @Override
    public java.util.Optional<ObjectMetadata> headObject(String storageKey) {
        return objectStorage.get(storageKey)
                .map(object -> new ObjectMetadata(object.contentType(), object.content().length,
                        object.durationSeconds()));
    }

    private String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }
}
