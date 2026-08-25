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
     * 활동 종류 플래그를 멱등하게 켠다. 정확히 한 종류만 true로 넘어온다.
     * attendance 스냅샷을 미러링하며, 중복 수신에도 안전하다.
     */
    @Modifying
    @Query(value = """
            INSERT INTO guardian_report_participations
                (elder_id, participation_date, training_done, greeting_read_done, memory_viewed_done, replied_done)
            VALUES (:elderId, :participationDate, :training, :greetingRead, :memoryViewed, :replied)
            ON CONFLICT (elder_id, participation_date) DO UPDATE SET
                training_done      = guardian_report_participations.training_done      OR EXCLUDED.training_done,
                greeting_read_done = guardian_report_participations.greeting_read_done OR EXCLUDED.greeting_read_done,
                memory_viewed_done = guardian_report_participations.memory_viewed_done OR EXCLUDED.memory_viewed_done,
                replied_done       = guardian_report_participations.replied_done       OR EXCLUDED.replied_done,
                updated_at = now()
            """, nativeQuery = true)
    int upsertActivity(@Param("elderId") UUID elderId,
                       @Param("participationDate") LocalDate participationDate,
                       @Param("training") boolean training,
                       @Param("greetingRead") boolean greetingRead,
                       @Param("memoryViewed") boolean memoryViewed,
                       @Param("replied") boolean replied);
}
