package com.memeboo2.haemi.elder.attendance.infrastructure;

import com.memeboo2.haemi.elder.attendance.domain.DailyParticipation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DailyParticipationRepository extends JpaRepository<DailyParticipation, UUID> {

    boolean existsByElderIdAndParticipationDate(UUID elderId, LocalDate participationDate);

    List<DailyParticipation> findByElderId(UUID elderId);
}
