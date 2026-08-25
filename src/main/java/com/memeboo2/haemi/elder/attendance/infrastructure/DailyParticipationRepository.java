package com.memeboo2.haemi.elder.attendance.infrastructure;

import com.memeboo2.haemi.elder.attendance.domain.DailyParticipation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DailyParticipationRepository extends JpaRepository<DailyParticipation, UUID> {

    boolean existsByElderIdAndParticipationDate(UUID elderId, LocalDate participationDate);

    long countByElderId(UUID elderId);

    List<DailyParticipation> findByElderId(UUID elderId);

    /**
     * 스트릭 계산용 — 최신 참여일부터 내림차순 날짜만 페이지 단위로 읽는다.
     */
    @Query("select p.participationDate from DailyParticipation p"
            + " where p.elderId = :elderId order by p.participationDate desc")
    List<LocalDate> findParticipationDatesDesc(@Param("elderId") UUID elderId, Pageable pageable);
}
