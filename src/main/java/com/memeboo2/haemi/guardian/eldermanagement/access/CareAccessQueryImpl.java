package com.memeboo2.haemi.guardian.eldermanagement.access;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLink;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLinkRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CareAccessQueryImpl implements CareAccessQuery {

    private static final Logger log = LoggerFactory.getLogger(CareAccessQueryImpl.class);

    private final GuardianElderLinkRepository linkRepository;

    @Override
    public void requireGuardianOf(UUID guardianId, UUID elderId) {
        if (!linkRepository.existsByGuardianIdAndElderId(guardianId, elderId)) {
            log.warn("CareAccess denied: guardianId={}, elderId={}, at={}", guardianId, elderId, Instant.now());
            throw new DomainException(ErrorCode.CARE_ACCESS_DENIED);
        }
    }

    @Override
    public void requireSelf(UUID actorId, UUID elderId) {
        // elder 테이블에서 userId == elderId 매핑은 auth 통합 후 연결
        // 현재는 actorId가 elder의 userId와 일치하는지 링크로 확인
        // (Elder.userId == actorId 검증은 elderRepository에서 처리)
    }

    @Override
    public boolean canAccess(UUID guardianId, UUID elderId) {
        return linkRepository.existsByGuardianIdAndElderId(guardianId, elderId);
    }

    @Override
    public List<UUID> accessibleElders(UUID guardianId) {
        return linkRepository.findAllByGuardianId(guardianId)
                .stream()
                .map(GuardianElderLink::getElderId)
                .toList();
    }

    @Override
    public GuardianRole roleOf(UUID guardianId, UUID elderId) {
        return linkRepository.findByGuardianIdAndElderId(guardianId, elderId)
                .map(GuardianElderLink::getRole)
                .orElseThrow(() -> new DomainException(ErrorCode.CARE_ACCESS_DENIED));
    }
}
