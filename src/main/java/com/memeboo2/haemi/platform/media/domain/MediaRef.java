package com.memeboo2.haemi.platform.media.domain;

import com.memeboo2.haemi.common.persistence.BaseEntity;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
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

    /** 음성의 클라이언트 선언 길이. 스토리지 검사 결과와 일치해야 한다. */
    @Column(name = "declared_duration_seconds")
    private Integer declaredDurationSeconds;

    /** 업로더 UUID (FK 없음 — 모듈 간 FK 금지) */
    @Column(nullable = false)
    private UUID uploaderId;

    /** presigned URL 만료 시각 */
    @Column(nullable = false)
    private Instant presignedUrlExpiresAt;

    /** 보관 만료 시각 (null = 무기한) */
    @Column
    private Instant retainUntil;

    /** 클라이언트가 계산한 SHA-256(hex 64자). 동일 업로더 중복 업로드 방지용. null = 미제공. */
    @Column(name = "content_hash", length = 64)
    private String contentHash;

    public static MediaRef pending(
            MediaType mediaType,
            String storageKey,
            String originalFilename,
            String contentType,
            long declaredSizeBytes,
            Integer declaredDurationSeconds,
            UUID uploaderId,
            Instant presignedUrlExpiresAt,
            Instant retainUntil,
            String contentHash) {

        MediaRef ref = new MediaRef();
        ref.mediaType = mediaType;
        ref.status = UploadStatus.PENDING;
        ref.storageKey = storageKey;
        ref.originalFilename = originalFilename;
        ref.contentType = contentType;
        ref.declaredSizeBytes = declaredSizeBytes;
        ref.declaredDurationSeconds = declaredDurationSeconds;
        ref.uploaderId = uploaderId;
        ref.presignedUrlExpiresAt = presignedUrlExpiresAt;
        ref.retainUntil = retainUntil;
        ref.contentHash = contentHash;
        return ref;
    }

    public void confirm(Instant now) {
        if (status == UploadStatus.CONFIRMED) {
            return; // 멱등: 이미 확정된 경우 재호출 허용
        }
        if (status == UploadStatus.EXPIRED) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "이미 처리된 업로드입니다.");
        }
        if (now.isAfter(presignedUrlExpiresAt)) {
            this.status = UploadStatus.EXPIRED;
            throw new DomainException(ErrorCode.INVALID_INPUT, "업로드 URL이 만료되었습니다.");
        }
        this.status = UploadStatus.CONFIRMED;
    }

    public boolean isOwnedBy(UUID actorId) {
        return uploaderId.equals(actorId);
    }

    /** 서버 변환(HEIC→JPEG 등) 후 저장 위치·타입·크기를 갱신한다. */
    public void replaceStorage(String newStorageKey, String newContentType, long newSizeBytes) {
        this.storageKey = newStorageKey;
        this.contentType = newContentType;
        this.declaredSizeBytes = newSizeBytes;
    }
}
