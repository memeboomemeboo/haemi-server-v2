package com.memeboo2.haemi.guardian.family.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.family.FamilyJoinCommand;
import com.memeboo2.haemi.common.persistence.ConstraintViolations;
import com.memeboo2.haemi.guardian.eldermanagement.domain.ElderRepository;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLink;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLinkRepository;
import com.memeboo2.haemi.guardian.family.domain.Family;
import com.memeboo2.haemi.guardian.family.domain.FamilyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 초대 코드로의 합류를 별도 트랜잭션(REQUIRES_NEW)에 가둔다.
 *
 * <p>합류의 마지막 방어선은 {@code uk_family_member_user} 유니크 제약이다 — 같은 보호자가
 * 서로 다른 가족에 동시에 합류/생성하면 선검사를 함께 통과하고, 그때 이 제약이 잡는다.
 * 그런데 이 위반을 <b>바깥 트랜잭션과 같은 커넥션</b>에서 잡으면 Postgres가 트랜잭션 전체를
 * abort시켜, catch 후 정상 흐름을 이어가거나 바깥이 커밋을 시도할 때 500이 된다.
 * REQUIRES_NEW로 분리하면 abort가 이 트랜잭션에 갇히고, {@code DomainException}으로 빠져나가
 * 호출자는 깨끗한 409를 받는다.
 *
 * <p>가족 락(R1 정원 검사)과 멤버 insert를 <b>같은 트랜잭션</b>에서 수행하므로,
 * 바깥이 가족 행을 FOR UPDATE로 잡은 채 자식 행을 insert하려다 FK 잠금에서 자기 자신과
 * 교착되는 문제도 생기지 않는다.
 */
@Component
@RequiredArgsConstructor
public class FamilyJoinSaver implements FamilyJoinCommand {

    private final FamilyRepository familyRepository;
    private final ElderRepository elderRepository;
    private final GuardianElderLinkRepository linkRepository;
    private final FamilyProperties props;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void join(UUID guardianId, String inviteCode) {
        joinInCurrentTransaction(guardianId, inviteCode);
    }

    /**
     * 새 보호자 회원가입 트랜잭션에서 사용한다. 계정 INSERT와 가족 멤버 INSERT를 함께 롤백해
     * 유효하지 않은 초대 코드로 계정만 생성되는 상태를 막는다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void joinInCurrentTransaction(UUID guardianId, String inviteCode) {
        Family family = familyRepository.findByInviteCodeForUpdate(inviteCode)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, "유효하지 않은 초대 코드입니다."));

        // 이미 다른 가족에 속해 있는지 (R2: 보호자는 한 가족만) — 흔한 경우를 선검사로 거른다.
        familyRepository.findByMembers_UserId(guardianId).ifPresent(f -> {
            throw new DomainException(ErrorCode.FAMILY_CAPACITY_EXCEEDED, "이미 가족에 속해 있습니다.");
        });

        // 보호자 상한 검증 (R1). 가족 락 아래에서 검사·삽입하므로 같은 가족 동시 합류는 직렬화된다.
        if (family.guardianCount() >= props.maxGuardians()) {
            throw new DomainException(ErrorCode.FAMILY_CAPACITY_EXCEEDED,
                    "가족당 보호자는 최대 " + props.maxGuardians() + "명까지 등록할 수 있습니다.");
        }

        family.addMember(guardianId);
        // 선검사만으로는 동시 합류/생성을 막지 못한다. DB 제약(uk_family_member_user)이 최종 방어선이다.
        try {
            familyRepository.flush();
        } catch (DataIntegrityViolationException violation) {
            if (ConstraintViolations.isViolationOf(violation, "uk_family_member_user")) {
                throw new DomainException(ErrorCode.FAMILY_CAPACITY_EXCEEDED, "이미 가족에 속해 있습니다.");
            }
            throw violation;
        }

        // 가족의 모든 어르신에 대해 링크 자동 생성 (R3) — 멤버 적재와 같은 트랜잭션에서 원자적으로.
        elderRepository.findAllByFamilyId(family.getId()).forEach(elder ->
                linkRepository.save(GuardianElderLink.create(guardianId, elder.getId()))
        );
    }
}
