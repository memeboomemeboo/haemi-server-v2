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

    /**
     * 활동 종류 플래그를 한 문장으로 원자적으로 켠다 (H2·PostgreSQL 공통 문법).
     * attendance 스냅샷을 미러링하며, 서로 다른 활동의 동시 갱신에도 각자 플래그만 OR로 켠다.
     * 반환값 1 = 지정한 종류가 false→true로 새로 켜짐, 0 = 행 없음 또는 이미 켜짐.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE guardian_report_participations SET
                training_done      = training_done      OR :training,
                greeting_read_done = greeting_read_done OR :greetingRead,
                memory_viewed_done = memory_viewed_done OR :memoryViewed,
                replied_done       = replied_done       OR :replied,
                updated_at = now()
            WHERE elder_id = :elderId AND participation_date = :participationDate
              AND ( (:training      AND NOT training_done)
                 OR (:greetingRead  AND NOT greeting_read_done)
                 OR (:memoryViewed  AND NOT memory_viewed_done)
                 OR (:replied       AND NOT replied_done) )
            """, nativeQuery = true)
    int markActivity(@Param("elderId") UUID elderId,
                     @Param("participationDate") LocalDate participationDate,
                     @Param("training") boolean training,
                     @Param("greetingRead") boolean greetingRead,
                     @Param("memoryViewed") boolean memoryViewed,
                     @Param("replied") boolean replied);
}
