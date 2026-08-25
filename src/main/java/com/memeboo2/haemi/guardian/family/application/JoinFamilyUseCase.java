package com.memeboo2.haemi.guardian.family.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JoinFamilyUseCase {

    private final FamilyJoinSaver familyJoinSaver;

    /**
     * 보호자가 초대 코드로 기존 가족에 합류 (D4). 가족의 모든 어르신에 대해 링크 자동 생성 (R3).
     * 어르신 계정은 SecurityConfig에서 /api/v1/guardian/** 자체를 호출할 수 없어 별도 검증이 불필요하다.
     *
     * <p>합류 본문은 {@link FamilyJoinSaver}가 REQUIRES_NEW 트랜잭션에서 수행한다 —
     * uk_family_member_user 위반이 나도 그 트랜잭션에만 갇혀 깨끗한 409로 나간다.
     */
    public void execute(UUID guardianId, String inviteCode) {
        familyJoinSaver.join(guardianId, inviteCode);
    }
}
