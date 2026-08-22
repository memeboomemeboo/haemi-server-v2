package com.memeboo2.haemi.guardian.api;

import java.util.List;
import java.util.UUID;

/**
 * 보호자–어르신 관계 기반 인가 관문. elderId를 다루는 모든 유스케이스의 첫 줄에서 호출.
 */
public interface CareAccessQuery {

    /** 링크가 없으면 CARE_ACCESS_DENIED(403). */
    void requireGuardianOf(UUID guardianId, UUID elderId);

    /** 어르신 본인 확인. 어르신 유스케이스의 첫 줄. */
    void requireSelf(UUID actorId, UUID elderId);

    boolean canAccess(UUID guardianId, UUID elderId);

    /** 목록 조회용. 반환된 것 외에는 노출 금지. */
    List<UUID> accessibleElders(UUID guardianId);

    /** 역할 라벨 (딸/아들/손녀 …). */
    GuardianRole roleOf(UUID guardianId, UUID elderId);
}
