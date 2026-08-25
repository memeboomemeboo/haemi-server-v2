package com.memeboo2.haemi.guardian.family.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface FamilyRepository extends JpaRepository<Family, UUID> {

    Optional<Family> findByMembers_UserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Family f LEFT JOIN FETCH f.members WHERE f.id = :familyId")
    Optional<Family> findByIdForUpdate(UUID familyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Family f LEFT JOIN FETCH f.members WHERE f.inviteCode = :inviteCode")
    Optional<Family> findByInviteCodeForUpdate(String inviteCode);
}
