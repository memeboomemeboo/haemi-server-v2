package com.memeboo2.haemi.guardian.dailycare.infrastructure;

import com.memeboo2.haemi.guardian.dailycare.domain.DailyCare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyCareRepository extends JpaRepository<DailyCare, UUID> {

    boolean existsByGuardianIdAndElderIdAndCareDate(UUID guardianId, UUID elderId, LocalDate careDate);

    @Query("SELECT d FROM DailyCare d WHERE d.elderId = :elderId AND d.careDate = :date AND d.retainUntil > :now ORDER BY d.createdAt DESC")
    List<DailyCare> findByElderIdAndDate(UUID elderId, LocalDate date, Instant now);

    @Query("SELECT d FROM DailyCare d WHERE d.elderId = :elderId AND d.careDate >= :from AND d.retainUntil > :now ORDER BY d.careDate DESC, d.createdAt DESC")
    List<DailyCare> findByElderIdSince(UUID elderId, LocalDate from, Instant now);

    Optional<DailyCare> findByGuardianIdAndElderIdAndCareDate(UUID guardianId, UUID elderId, LocalDate careDate);

    /** 보낸 사람 본인의 전송 이력 (R6). 보관 만료 여부와 무관하게 전부 반환한다. */
    List<DailyCare> findByGuardianIdAndElderIdOrderByCareDateDescCreatedAtDesc(UUID guardianId, UUID elderId);
}
