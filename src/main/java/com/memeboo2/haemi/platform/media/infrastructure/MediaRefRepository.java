package com.memeboo2.haemi.platform.media.infrastructure;

import com.memeboo2.haemi.platform.media.domain.MediaRef;
import com.memeboo2.haemi.platform.media.domain.UploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MediaRefRepository extends JpaRepository<MediaRef, UUID> {

    /** 동일 업로더가 이미 확정한 동일 해시 미디어 (중복 업로드 방지). */
    Optional<MediaRef> findFirstByUploaderIdAndContentHashAndStatus(
            UUID uploaderId, String contentHash, UploadStatus status);
}
