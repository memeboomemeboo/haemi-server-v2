package com.memeboo2.haemi.guardian.eldermanagement.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GuardianElderLinkRepository extends JpaRepository<GuardianElderLink, UUID> {

    Optional<GuardianElderLink> findByGuardianIdAndElderId(UUID guardianId, UUID elderId);

    List<GuardianElderLink> findAllByGuardianId(UUID guardianId);

    List<GuardianElderLink> findAllByElderId(UUID elderId);

    long countByElderId(UUID elderId);

    boolean existsByGuardianIdAndElderId(UUID guardianId, UUID elderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM GuardianElderLink l WHERE l.elderId = :elderId")
    List<GuardianElderLink> findAllByElderIdForUpdate(UUID elderId);
}
