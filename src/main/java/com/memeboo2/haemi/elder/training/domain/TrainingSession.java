package com.memeboo2.haemi.elder.training.domain;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** 어르신별 인지 훈련 진행 상태. 미완료 세션은 날짜가 바뀌어도 이어진다. */
@Entity
@Table(
        name = "elder_training_sessions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_training_sessions_elder_date",
                        columnNames = {"elder_id", "session_date"}
                ),
                @UniqueConstraint(
                        name = "uk_training_sessions_active_elder",
                        columnNames = "active_elder_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainingSession extends BaseEntity {

    @Column(nullable = false)
    private UUID elderId;

    /** 진행 중일 때만 elderId를 보관해, DB에서 어르신당 진행 중 세션 하나를 보장한다. */
    @Column
    private UUID activeElderId;

    /** 세션이 시작된 KST 날짜. 같은 날짜의 동시 시작을 DB 제약으로 막는다. */
    @Column(nullable = false)
    private LocalDate sessionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private QuestionType currentStep;

    @Column(nullable = false, updatable = false)
    private Instant startedAt;

    @Column
    private Instant completedAt;

    public static TrainingSession start(UUID elderId, Instant startedAt, LocalDate sessionDate) {
        TrainingSession session = new TrainingSession();
        session.assignIdIfAbsent();
        session.elderId = elderId;
        session.activeElderId = elderId;
        session.sessionDate = sessionDate;
        session.status = SessionStatus.IN_PROGRESS;
        session.currentStep = QuestionType.ORIENTATION;
        session.startedAt = startedAt;
        return session;
    }

    /** 현재 단계만 완료할 수 있으며, 마지막 단계에서만 세션이 완료된다. */
    public void completeCurrentStep(QuestionType step, Instant completedAt) {
        if (status == SessionStatus.COMPLETED) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "이미 완료한 인지 훈련 세션입니다.");
        }
        if (currentStep != step) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "현재 진행 중인 훈련 단계가 아닙니다.");
        }

        QuestionType nextStep = nextOf(step);
        if (nextStep != null) {
            currentStep = nextStep;
            return;
        }

        status = SessionStatus.COMPLETED;
        currentStep = null;
        this.completedAt = completedAt;
        activeElderId = null;
    }

    private QuestionType nextOf(QuestionType step) {
        return switch (step) {
            case ORIENTATION -> QuestionType.RECALL;
            case RECALL -> QuestionType.LANGUAGE;
            case LANGUAGE -> QuestionType.DELAYED_RECALL;
            case DELAYED_RECALL -> null;
        };
    }
}
