package com.memeboo2.haemi.elder.training.infrastructure;

import com.memeboo2.haemi.elder.training.domain.SessionStatus;
import com.memeboo2.haemi.elder.training.domain.TrainingSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingSessionRepository extends JpaRepository<TrainingSession, UUID> {

    /** 오늘의 기록 타임라인(#100 M2): 특정 날짜에 완료된 세션들. */
    List<TrainingSession> findByElderIdAndSessionDateAndStatus(
            UUID elderId, LocalDate sessionDate, SessionStatus status);

    /** 같은 세션의 중복 응답이 문항을 두 번 넘기지 않도록 진행 상태를 잠근다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s FROM TrainingSession s
            WHERE s.elderId = :elderId AND s.status = :status
            ORDER BY s.startedAt ASC
            """)
    Optional<TrainingSession> findFirstByElderIdAndStatusForUpdate(
            @Param("elderId") UUID elderId,
            @Param("status") SessionStatus status
    );

    Optional<TrainingSession> findFirstByElderIdAndStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
            UUID elderId,
            SessionStatus status,
            Instant startInclusive,
            Instant endExclusive
    );
}
