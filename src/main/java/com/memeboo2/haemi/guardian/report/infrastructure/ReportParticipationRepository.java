package com.memeboo2.haemi.guardian.report.infrastructure;

import com.memeboo2.haemi.guardian.report.domain.ReportParticipation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReportParticipationRepository extends JpaRepository<ReportParticipation, UUID> {

    boolean existsByElderIdAndParticipationDate(UUID elderId, LocalDate participationDate);

    List<ReportParticipation> findByElderId(UUID elderId);

    List<ReportParticipation> findByElderIdAndParticipationDateGreaterThanEqual(UUID elderId, LocalDate from);

    /** 존재하면 아무것도 하지 않는 원자적 삽입 — exists-then-insert의 REQUIRES_NEW 커밋 실패를 피한다. */
    @Modifying
    @Query(value = """
            INSERT INTO guardian_report_participations (elder_id, participation_date)
            VALUES (:elderId, :participationDate)
            ON CONFLICT (elder_id, participation_date) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("elderId") UUID elderId, @Param("participationDate") LocalDate participationDate);
}
