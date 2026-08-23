package com.memeboo2.haemi.guardian.eldermanagement.access;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.eldermanagement.domain.Elder;
import com.memeboo2.haemi.guardian.eldermanagement.domain.ElderRepository;
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
    private final ElderRepository elderRepository;

    @Override
    public void requireGuardianOf(UUID guardianId, UUID elderId) {
        if (!linkRepository.existsByGuardianIdAndElderId(guardianId, elderId)) {
            log.warn("CareAccess denied: guardianId={}, elderId={}, at={}", guardianId, elderId, Instant.now());
            throw new DomainException(ErrorCode.CARE_ACCESS_DENIED);
        }
    }

    @Override
    public void requireSelf(UUID actorId, UUID elderId) {
        Elder elder = elderRepository.findById(elderId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!elder.getUserId().equals(actorId)) {
            log.warn("CareAccess denied (self): actorId={}, elderId={}, at={}", actorId, elderId, Instant.now());
            throw new DomainException(ErrorCode.CARE_ACCESS_DENIED);
        }
    }

    @Override
    public UUID elderIdForUser(UUID userId) {
        return elderRepository.findByUserId(userId)
                .map(Elder::getId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND));
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
