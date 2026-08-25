package com.memeboo2.haemi.elder.attendance.infrastructure;

import com.memeboo2.haemi.elder.attendance.domain.DailyParticipation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DailyParticipationRepository extends JpaRepository<DailyParticipation, UUID> {

    boolean existsByElderIdAndParticipationDate(UUID elderId, LocalDate participationDate);

    boolean existsByTrainingSessionId(UUID trainingSessionId);

    long countByElderId(UUID elderId);

    @Query("""
            SELECT p.participationDate FROM DailyParticipation p
            WHERE p.elderId = :elderId AND p.participationDate <= :through
            ORDER BY p.participationDate DESC
            """)
    List<LocalDate> findParticipationDatesThrough(UUID elderId, LocalDate through);
}
