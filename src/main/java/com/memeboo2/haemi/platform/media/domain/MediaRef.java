package com.memeboo2.haemi.platform.media.domain;

import com.memeboo2.haemi.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "media_uploads")
public class MediaRef extends BaseEntity {

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MediaType mediaType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UploadStatus status;

    /** S3 오브젝트 키 (presigned URL 발급 시 서버가 생성) */
    @Column(nullable = false, length = 500)
    private String storageKey;

    /** 원본 파일명 — 검증 로그용 */
    @Column(nullable = false, length = 255)
    private String originalFilename;

    /** Content-Type (허용 목록과 대조) */
    @Column(nullable = false, length = 100)
    private String contentType;

    /** 클라이언트 선언 크기 (bytes) — 실제 크기는 스토리지에서 확정 */
    @Column(nullable = false)
    private long declaredSizeBytes;

    /** 업로더 UUID (FK 없음 — 모듈 간 FK 금지) */
    @Column(nullable = false)
    private UUID uploaderId;

    /** presigned URL 만료 시각 */
    @Column(nullable = false)
    private Instant presignedUrlExpiresAt;

    /** 보관 만료 시각 (null = 무기한) */
    @Column
    private Instant retainUntil;

    public static MediaRef pending(
            MediaType mediaType,
            String storageKey,
            String originalFilename,
            String contentType,
            long declaredSizeBytes,
            UUID uploaderId,
            Instant presignedUrlExpiresAt,
            Instant retainUntil) {

        MediaRef ref = new MediaRef();
        ref.mediaType = mediaType;
        ref.status = UploadStatus.PENDING;
        ref.storageKey = storageKey;
        ref.originalFilename = originalFilename;
        ref.contentType = contentType;
        ref.declaredSizeBytes = declaredSizeBytes;
        ref.uploaderId = uploaderId;
        ref.presignedUrlExpiresAt = presignedUrlExpiresAt;
        ref.retainUntil = retainUntil;
        return ref;
    }

    public void confirm(Instant now) {
        if (status != UploadStatus.PENDING) {
            throw new IllegalStateException("already " + status);
        }
        if (now.isAfter(presignedUrlExpiresAt)) {
            this.status = UploadStatus.EXPIRED;
            throw new IllegalStateException("presigned URL expired");
        }
        this.status = UploadStatus.CONFIRMED;
    }

    public boolean isOwnedBy(UUID actorId) {
        return uploaderId.equals(actorId);
    }
}
