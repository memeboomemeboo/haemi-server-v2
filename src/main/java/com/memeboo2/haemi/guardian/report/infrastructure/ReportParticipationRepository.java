package com.memeboo2.haemi.guardian.report.infrastructure;

import com.memeboo2.haemi.guardian.report.domain.ReportParticipation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportParticipationRepository extends JpaRepository<ReportParticipation, UUID> {

    boolean existsByElderIdAndParticipationDate(UUID elderId, LocalDate participationDate);

    Optional<ReportParticipation> findByElderIdAndParticipationDate(UUID elderId, LocalDate participationDate);

    List<ReportParticipation> findByElderId(UUID elderId);

    List<ReportParticipation> findByElderIdAndParticipationDateGreaterThanEqual(UUID elderId, LocalDate from);
}
