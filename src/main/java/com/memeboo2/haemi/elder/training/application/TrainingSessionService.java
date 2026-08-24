package com.memeboo2.haemi.elder.training.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.event.TrainingSessionCompleted;
import com.memeboo2.haemi.common.security.ElderAccessChecked;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.elder.training.domain.QuestionType;
import com.memeboo2.haemi.elder.training.domain.SessionStatus;
import com.memeboo2.haemi.elder.training.domain.TrainingSession;
import com.memeboo2.haemi.elder.training.infrastructure.TrainingSessionRepository;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/** CIST-TRN-001의 하루 1회·이어하기 정책을 적용한다. */
@Service
@RequiredArgsConstructor
public class TrainingSessionService implements TrainingSessionUseCase {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final TrainingSessionRepository trainingSessionRepository;
    private final HaemiClock clock;
    private final CareAccessQuery careAccessQuery;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @ElderAccessChecked
    @Transactional
    public TrainingSessionView enter(UUID elderUserId) {
        UUID elderId = requireElderId(elderUserId);
        return trainingSessionRepository
                .findFirstByElderIdAndStatusOrderByStartedAtAsc(elderId, SessionStatus.IN_PROGRESS)
                .map(TrainingSessionView::from)
                .orElseGet(() -> completedTodayOrStart(elderId));
    }

    @Override
    @ElderAccessChecked
    @Transactional
    public TrainingSessionView completeCurrentQuestion(UUID elderUserId, QuestionType questionType) {
        UUID elderId = requireElderId(elderUserId);
        TrainingSession session = trainingSessionRepository
                .findFirstByElderIdAndStatusOrderByStartedAtAsc(elderId, SessionStatus.IN_PROGRESS)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, "진행 중인 인지 훈련 세션이 없습니다."));

        session.completeCurrentQuestion(questionType, clock.now());
        TrainingSession saved = trainingSessionRepository.saveAndFlush(session);
        if (saved.getStatus() == SessionStatus.COMPLETED) {
            eventPublisher.publishEvent(new TrainingSessionCompleted(
                    saved.getId(), saved.getElderId(), saved.getSessionDate(), clock.today(), saved.getCompletedAt()));
        }
        return TrainingSessionView.from(saved);
    }

    private TrainingSessionView completedTodayOrStart(UUID elderId) {
        LocalDate today = clock.today();
        Instant startOfToday = today.atStartOfDay(KST).toInstant();
        Instant startOfTomorrow = today.plusDays(1).atStartOfDay(KST).toInstant();

        return trainingSessionRepository
                .findFirstByElderIdAndStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
                        elderId, SessionStatus.COMPLETED, startOfToday, startOfTomorrow)
                .map(TrainingSessionView::from)
                .orElseGet(() -> start(elderId, today));
    }

    private TrainingSessionView start(UUID elderId, LocalDate today) {
        TrainingSession session = TrainingSession.start(elderId, clock.now(), today);
        try {
            return TrainingSessionView.from(trainingSessionRepository.saveAndFlush(session));
        } catch (DataIntegrityViolationException ex) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "같은 날 인지 훈련 세션을 중복해서 시작할 수 없습니다.");
        }
    }

    private UUID requireElderId(UUID elderUserId) {
        UUID elderId = careAccessQuery.elderIdForUser(elderUserId);
        careAccessQuery.requireSelf(elderUserId, elderId);
        return elderId;
    }
}
