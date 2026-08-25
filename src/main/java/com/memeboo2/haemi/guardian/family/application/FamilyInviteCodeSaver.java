package com.memeboo2.haemi.guardian.family.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.family.domain.Family;
import com.memeboo2.haemi.guardian.family.domain.FamilyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가족 insert를 별도 트랜잭션에 가둔다.
 * 같은 트랜잭션에서 save하면 unique 위반 시 Postgres가 트랜잭션 전체를 abort시켜
 * 재시도도 함께 실패한다. REQUIRES_NEW로 분리하면 바깥 트랜잭션은 멀쩡하다.
 * 충돌은 값을 반환하지 않고 예외로 빠져나간다 — 실패한 트랜잭션은 이미 rollback-only여서
 * 정상 반환하면 커밋 시점에 UnexpectedRollbackException으로 500이 된다.
 */
@Component
@RequiredArgsConstructor
public class FamilyInviteCodeSaver {

    private final FamilyRepository familyRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(Family family) {
        try {
            familyRepository.saveAndFlush(family);
        } catch (DataIntegrityViolationException e) {
            if (isGuardianAlreadyInFamily(e)) {
                // R2: 보호자 1인 1가족. 동시 생성/합류로 선검사를 통과한 경우 DB 제약이 잡는다.
                throw new DomainException(ErrorCode.FAMILY_CAPACITY_EXCEEDED, "이미 가족에 속해 있습니다.");
            }
            throw new InviteCodeConflictException(e);
        }
    }

    private boolean isGuardianAlreadyInFamily(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains("uk_family_member_user");
    }
}
