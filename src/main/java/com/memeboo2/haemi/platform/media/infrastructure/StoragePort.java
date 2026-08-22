package com.memeboo2.haemi.platform.media.infrastructure;

import com.memeboo2.haemi.platform.media.domain.MediaType;

import java.net.URI;

public interface StoragePort {

    /**
     * @return presigned PUT URL (클라이언트가 직접 스토리지에 업로드)
     */
    URI generatePresignedPutUrl(String storageKey, String contentType, long expirySeconds);

    /**
     * @return 서빙 URL (확정된 미디어에 접근)
     */
    URI generateServingUrl(String storageKey);

    String buildStorageKey(MediaType mediaType, String originalFilename);
}
