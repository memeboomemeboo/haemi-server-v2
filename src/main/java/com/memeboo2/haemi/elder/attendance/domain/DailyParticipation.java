package com.memeboo2.haemi.elder.attendance.domain;

import com.memeboo2.haemi.common.event.TrainingSessionCompleted;
import com.memeboo2.haemi.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** 출석의 단일 원천. 세션 완료 이벤트는 이 레코드를 한 번만 만든다. */
@Entity
@Table(name = "elder_daily_participations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_daily_participation_session", columnNames = "training_session_id"),
        @UniqueConstraint(name = "uk_daily_participation_elder_date", columnNames = {"elder_id", "participation_date"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyParticipation extends BaseEntity {

    @Column(nullable = false)
    private UUID trainingSessionId;

    @Column(nullable = false)
    private UUID elderId;

    @Column(nullable = false)
    private LocalDate participationDate;

    @Column(nullable = false)
    private Instant completedAt;

    public static DailyParticipation from(TrainingSessionCompleted event) {
        DailyParticipation participation = new DailyParticipation();
        participation.assignIdIfAbsent();
        participation.trainingSessionId = event.trainingSessionId();
        participation.elderId = event.elderId();
        participation.participationDate = event.completedDate();
        participation.completedAt = event.completedAt();
        return participation;
    }
}
