package com.memeboo2.haemi.guardian.family.application;

import com.memeboo2.haemi.guardian.family.domain.Family;
import com.memeboo2.haemi.guardian.family.domain.FamilyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 초대 코드 unique 충돌을 별도 트랜잭션에 가둔다.
 * CreateFamilyUseCase의 트랜잭션 안에서 그대로 save하면, 코드 생성과 실제 insert 사이에
 * 다른 요청이 같은 코드를 먼저 커밋했을 때 unique 위반이 발생하고,
 * Postgres는 그 순간부터 트랜잭션 전체를 abort 상태로 만들어 이후 재시도도 함께 실패한다.
 * REQUIRES_NEW로 이 insert만 별도 커넥션에서 시도하면, 실패해도 바깥 트랜잭션은 멀쩡하다.
 */
@Component
@RequiredArgsConstructor
public class FamilyInviteCodeSaver {

    private final FamilyRepository familyRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean trySave(Family family) {
        try {
            familyRepository.saveAndFlush(family);
            return true;
        } catch (DataIntegrityViolationException duplicateInviteCode) {
            return false;
        }
    }
}
