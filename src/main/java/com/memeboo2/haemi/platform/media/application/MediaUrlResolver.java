package com.memeboo2.haemi.platform.media.application;

import com.memeboo2.haemi.platform.media.infrastructure.StoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/** 영구 storage key와 배포 전 저장된 serving URL을 현재 serving URL로 변환한다. */
@Component
@RequiredArgsConstructor
public class MediaUrlResolver {

    private final StoragePort storage;

    @Value("${haemi.media.storage.bucket:}")
    private String bucket;

    public String toServingUrl(String value) {
        if (value == null || value.isBlank()) return null;
        String key = extractStorageKey(value);
        return key == null ? value : storage.generateServingUrl(key).toString();
    }

    String extractStorageKey(String value) {
        if (!value.startsWith("http://") && !value.startsWith("https://")) return value;
        try {
            URI uri = URI.create(value);
            String localKey = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("key");
            if (localKey != null && !localKey.isBlank()) return localKey;

            String path = uri.getPath();
            if (path == null || path.isBlank()) return null;
            path = path.startsWith("/") ? path.substring(1) : path;
            String host = uri.getHost();
            if (host == null || uri.getRawQuery() == null) return null;
            // AWS S3뿐 아니라 R2 등 S3 호환 스토리지의 virtual-hosted URL도 지원한다.
            if (!bucket.isBlank() && host.startsWith(bucket + ".")) return path;
            if (!bucket.isBlank() && path.startsWith(bucket + "/")) return path.substring(bucket.length() + 1);
        } catch (IllegalArgumentException ignored) {
            // 외부 URL은 보존한다.
        }
        return null;
    }
}
