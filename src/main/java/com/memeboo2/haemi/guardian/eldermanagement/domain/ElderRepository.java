package com.memeboo2.haemi.guardian.eldermanagement.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ElderRepository extends JpaRepository<Elder, UUID> {

    List<Elder> findAllByFamilyId(UUID familyId);

    Optional<Elder> findByUserId(UUID userId);

    long countByFamilyId(UUID familyId);
}
