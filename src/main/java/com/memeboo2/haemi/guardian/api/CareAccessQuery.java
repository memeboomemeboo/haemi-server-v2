package com.memeboo2.haemi.guardian.api;

import java.util.List;
import java.util.UUID;

/**
 * 보호자–어르신 관계 기반 인가 관문. elderId를 다루는 모든 유스케이스의 첫 줄에서 호출.
 */
public interface CareAccessQuery {

    /** 링크가 없으면 CARE_ACCESS_DENIED(403). */
    void requireGuardianOf(UUID guardianId, UUID elderId);

    /**
     * 인증 Account ID를 guardian_elders의 도메인 ID로 해석한다.
     * 해당 어르신이 없으면 RESOURCE_NOT_FOUND로 fail-closed 하므로, 어르신 유스케이스의 본인 인가 관문이다.
     * (elderId는 이 호출로만 얻으므로 별도 본인 재확인은 항진식이다 — #137)
     */
    UUID elderIdForUser(UUID userId);

    boolean canAccess(UUID guardianId, UUID elderId);

    /** 목록 조회용. 반환된 것 외에는 노출 금지. */
    List<UUID> accessibleElders(UUID guardianId);

    /** 역할 라벨 (딸/아들/손녀 …). */
    GuardianRole roleOf(UUID guardianId, UUID elderId);

    /** 정기 리포트 발송 등 배치용 전체 링크 열거. 인가 관문 아님 — 스케줄러 전용. */
    List<CareLink> allLinks();

    record CareLink(UUID guardianId, UUID elderId, GuardianRole role) {}
}
