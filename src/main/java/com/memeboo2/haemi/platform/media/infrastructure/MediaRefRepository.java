package com.memeboo2.haemi.platform.media.infrastructure;

import com.memeboo2.haemi.platform.media.domain.MediaRef;
import com.memeboo2.haemi.platform.media.domain.MediaType;
import com.memeboo2.haemi.platform.media.domain.UploadStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MediaRefRepository extends JpaRepository<MediaRef, UUID> {

    /** 동일 업로더가 같은 용도로 이미 확정한 동일 해시 미디어 (중복 업로드 방지). */
    Optional<MediaRef> findFirstByUploaderIdAndMediaTypeAndContentHashAndStatus(
            UUID uploaderId, MediaType mediaType, String contentHash, UploadStatus status);

    /**
     * 동일 해시 확정을 직렬화하기 위한 공통 잠금 행이다. 모든 후보가 같은 첫 행을 잠그므로
     * 두 PENDING MediaRef가 동시에 CONFIRMED로 바뀌어 부분 유니크 인덱스와 충돌하지 않는다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MediaRef> findFirstByUploaderIdAndMediaTypeAndContentHashOrderByIdAsc(
            UUID uploaderId, MediaType mediaType, String contentHash);
}
