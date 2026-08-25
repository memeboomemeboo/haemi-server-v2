package com.memeboo2.haemi.elder.attendance.infrastructure;

import com.memeboo2.haemi.elder.attendance.domain.DailyParticipation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DailyParticipationRepository extends JpaRepository<DailyParticipation, UUID> {

    boolean existsByElderIdAndParticipationDate(UUID elderId, LocalDate participationDate);

    List<DailyParticipation> findByElderId(UUID elderId);

    List<DailyParticipation> findByElderIdAndParticipationDateGreaterThanEqual(UUID elderId, LocalDate from);

    /**
     * 스트릭 계산용 — 최신 참여일부터 내림차순 날짜만 투영해 읽는다.
     * 엔티티 전량 대신 날짜만 읽고, 호출부가 첫 공백에서 조기 종료하도록 정렬해 둔다.
     */
    @Query("select p.participationDate from DailyParticipation p"
            + " where p.elderId = :elderId order by p.participationDate desc")
    List<LocalDate> findParticipationDatesDesc(@Param("elderId") UUID elderId);

    /**
     * 존재하면 아무것도 하지 않는 원자적 삽입. exists-then-insert는 두 이벤트가 동시에
     * exists 검사를 통과하면 unique 위반으로 REQUIRES_NEW 트랜잭션 자체가 실패한다 (커밋 불가).
     * 반환값이 1이면 새로 적재된 것 — 그때만 하위 이벤트를 발행해야 중복 발행을 막는다.
     */
    @Modifying
    @Query(value = """
            INSERT INTO elder_attendance_daily_participations (elder_id, participation_date, training_done)
            VALUES (:elderId, :participationDate, true)
            ON CONFLICT (elder_id, participation_date) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("elderId") UUID elderId, @Param("participationDate") LocalDate participationDate);

    /**
     * 활동 종류 플래그를 멱등하게 켠다. 정확히 한 종류의 boolean만 true로 넘어온다.
     * 반환값 1 = 새 날짜가 삽입됐거나 해당 종류 플래그가 false→true로 새로 켜짐 (그때만 이벤트 발행).
     * 반환값 0 = 이미 그 종류가 기록돼 있어 변화 없음.
     * DO UPDATE의 WHERE로 "새로 켜질 때만" 갱신되도록 해 중복 발행을 막는다.
     */
    @Modifying
    @Query(value = """
            INSERT INTO elder_attendance_daily_participations
                (elder_id, participation_date, training_done, greeting_read_done, memory_viewed_done, replied_done)
            VALUES (:elderId, :participationDate, :training, :greetingRead, :memoryViewed, :replied)
            ON CONFLICT (elder_id, participation_date) DO UPDATE SET
                training_done      = elder_attendance_daily_participations.training_done      OR EXCLUDED.training_done,
                greeting_read_done = elder_attendance_daily_participations.greeting_read_done OR EXCLUDED.greeting_read_done,
                memory_viewed_done = elder_attendance_daily_participations.memory_viewed_done OR EXCLUDED.memory_viewed_done,
                replied_done       = elder_attendance_daily_participations.replied_done       OR EXCLUDED.replied_done,
                updated_at = now()
            WHERE
                (EXCLUDED.training_done      AND NOT elder_attendance_daily_participations.training_done) OR
                (EXCLUDED.greeting_read_done AND NOT elder_attendance_daily_participations.greeting_read_done) OR
                (EXCLUDED.memory_viewed_done AND NOT elder_attendance_daily_participations.memory_viewed_done) OR
                (EXCLUDED.replied_done       AND NOT elder_attendance_daily_participations.replied_done)
            """, nativeQuery = true)
    int upsertActivity(@Param("elderId") UUID elderId,
                       @Param("participationDate") LocalDate participationDate,
                       @Param("training") boolean training,
                       @Param("greetingRead") boolean greetingRead,
                       @Param("memoryViewed") boolean memoryViewed,
                       @Param("replied") boolean replied);
}
