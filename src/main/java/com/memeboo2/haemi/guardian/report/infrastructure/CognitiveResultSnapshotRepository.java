package com.memeboo2.haemi.guardian.report.infrastructure;

import com.memeboo2.haemi.guardian.report.domain.CognitiveResultSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CognitiveResultSnapshotRepository extends JpaRepository<CognitiveResultSnapshot, UUID> {

    List<CognitiveResultSnapshot> findByElderIdAndSessionDateGreaterThanEqual(UUID elderId, LocalDate from);

    /**
     * PostgreSQL 15+의 MERGE를 사용한다(운영 DB: PostgreSQL 16).
     * 재전달은 (session_id, cognitive_area) 유일 키로 원자적으로 무시하며, 재채점·정정은 별도 버전 정책이 정해지기 전까지 갱신하지 않는다.
     */
    @Modifying
    @Query(value = """
            MERGE INTO guardian_report_cognitive_results AS target
            USING (VALUES (:id, :elderId, :sessionId, :sessionDate, :area, :scoredAnswerCount, :correctAnswerCount))
                AS source(id, elder_id, session_id, session_date, cognitive_area, scored_answer_count, correct_answer_count)
            ON target.session_id = source.session_id AND target.cognitive_area = source.cognitive_area
            WHEN NOT MATCHED THEN
                INSERT (id, elder_id, session_id, session_date, cognitive_area, scored_answer_count, correct_answer_count,
                        created_at, updated_at)
                VALUES (source.id, source.elder_id, source.session_id, source.session_date, source.cognitive_area,
                        source.scored_answer_count, source.correct_answer_count, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("elderId") UUID elderId,
            @Param("sessionId") UUID sessionId,
            @Param("sessionDate") LocalDate sessionDate,
            @Param("area") String area,
            @Param("scoredAnswerCount") int scoredAnswerCount,
            @Param("correctAnswerCount") int correctAnswerCount
    );
}
