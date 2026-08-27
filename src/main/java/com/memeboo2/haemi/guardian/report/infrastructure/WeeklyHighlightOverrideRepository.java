package com.memeboo2.haemi.guardian.report.infrastructure;

import com.memeboo2.haemi.guardian.report.domain.WeeklyHighlightOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface WeeklyHighlightOverrideRepository extends JpaRepository<WeeklyHighlightOverride, UUID> {

    Optional<WeeklyHighlightOverride> findByElderIdAndWeekStart(UUID elderId, LocalDate weekStart);
}
