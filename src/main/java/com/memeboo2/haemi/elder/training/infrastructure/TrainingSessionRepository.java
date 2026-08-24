package com.memeboo2.haemi.elder.training.infrastructure;

import com.memeboo2.haemi.elder.training.domain.SessionStatus;
import com.memeboo2.haemi.elder.training.domain.TrainingSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TrainingSessionRepository extends JpaRepository<TrainingSession, UUID> {

    Optional<TrainingSession> findFirstByElderIdAndStatusOrderByStartedAtAsc(UUID elderId, SessionStatus status);

    Optional<TrainingSession> findFirstByElderIdAndStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
            UUID elderId,
            SessionStatus status,
            Instant startInclusive,
            Instant endExclusive
    );
}
