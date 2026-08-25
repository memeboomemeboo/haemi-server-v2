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
            INSERT INTO elder_attendance_daily_participations (elder_id, participation_date)
            VALUES (:elderId, :participationDate)
            ON CONFLICT (elder_id, participation_date) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("elderId") UUID elderId, @Param("participationDate") LocalDate participationDate);
}
