package com.memeboo2.haemi.platform.api;

import java.net.URI;
import java.util.UUID;

/**
 * platform/media의 업로드 확정 계약.
 * guardian/memory 등 다른 그룹은 이 인터페이스를 통해서만 platform/media에 접근한다.
 */
public interface MediaUploadCommand {

    /**
     * presigned URL로 업로드한 미디어를 확정하고 서빙 URL을 반환한다.
     */
    URI confirmUpload(UUID actorId, UUID mediaRefId);

    /** 확정할 미디어의 용도를 검증한다. */
    URI confirmUpload(UUID actorId, UUID mediaRefId, MediaPurpose expectedPurpose);

    /** 확정된 음성의 서버 검증 길이가 요청 길이와 같은지도 검증한다. */
    URI confirmUpload(UUID actorId, UUID mediaRefId, MediaPurpose expectedPurpose, Integer expectedDurationSeconds);

    /** 확정된 음성 응답의 재생 시간을 응답 레코드에 고정하기 위한 업로드 메타데이터 조회. */
    Integer declaredDurationSeconds(UUID mediaRefId);

    /** 추억 하나에 첨부 가능한 이미지 최대 장수 (haemi.media.image.memory-max-count). */
    int memoryImageMaxCount();
}
