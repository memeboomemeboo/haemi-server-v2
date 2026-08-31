package com.memeboo2.haemi.elder.attendance.infrastructure;

import com.memeboo2.haemi.elder.attendance.domain.DailyParticipation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DailyParticipationRepository extends JpaRepository<DailyParticipation, UUID> {

    boolean existsByElderIdAndParticipationDate(UUID elderId, LocalDate participationDate);

    long countByElderId(UUID elderId);

    /** 지정 날짜를 제외한 참여일 수. 완료 직후 배지 집계에서 '오늘'을 항상 1로 더하기 위해 사용한다. */
    long countByElderIdAndParticipationDateNot(UUID elderId, LocalDate participationDate);

    List<DailyParticipation> findByElderId(UUID elderId);

    List<DailyParticipation> findByElderIdAndParticipationDateGreaterThanEqual(UUID elderId, LocalDate from);

    /**
     * 스트릭 계산용 — 최신 참여일부터 내림차순 날짜만 페이지 단위로 읽는다.
     */
    @Query("select p.participationDate from DailyParticipation p"
            + " where p.elderId = :elderId order by p.participationDate desc")
    List<LocalDate> findParticipationDatesDesc(@Param("elderId") UUID elderId, Pageable pageable);

    /**
     * 활동 종류 플래그를 한 문장으로 원자적으로 켠다 (H2·PostgreSQL 공통 문법).
     * 서로 다른 활동이 같은 행을 동시에 갱신해도 각자의 플래그만 OR로 켜지므로,
     * read-modify-write처럼 다른 종류 플래그를 stale 값으로 덮어쓰지 않는다.
     * 반환값 1 = 지정한 종류가 false→true로 새로 켜짐 (그때만 이벤트 발행).
     * 반환값 0 = 행이 없거나 이미 그 종류가 켜져 있어 변화 없음.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE elder_attendance_daily_participations SET
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
