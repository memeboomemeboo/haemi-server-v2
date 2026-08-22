package com.memeboo2.haemi.guardian.eldermanagement.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GuardianElderLinkRepository extends JpaRepository<GuardianElderLink, UUID> {

    Optional<GuardianElderLink> findByGuardianIdAndElderId(UUID guardianId, UUID elderId);

    List<GuardianElderLink> findAllByGuardianId(UUID guardianId);

    List<GuardianElderLink> findAllByElderId(UUID elderId);

    long countByElderId(UUID elderId);

    boolean existsByGuardianIdAndElderId(UUID guardianId, UUID elderId);
}
