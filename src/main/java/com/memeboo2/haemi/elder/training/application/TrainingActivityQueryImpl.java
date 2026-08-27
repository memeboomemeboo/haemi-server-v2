package com.memeboo2.haemi.elder.training.application;

import com.memeboo2.haemi.elder.training.domain.SessionStatus;
import com.memeboo2.haemi.elder.training.infrastructure.TrainingSessionRepository;
import com.memeboo2.haemi.guardian.api.TrainingActivityQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrainingActivityQueryImpl implements TrainingActivityQuery {

    private final TrainingSessionRepository sessionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CompletedSession> completedOn(UUID elderId, LocalDate date) {
        return sessionRepository
                .findByElderIdAndSessionDateAndStatus(elderId, date, SessionStatus.COMPLETED).stream()
                .filter(s -> s.getCompletedAt() != null)
                .map(s -> new CompletedSession(s.getCompletedAt()))
                .toList();
    }
}
