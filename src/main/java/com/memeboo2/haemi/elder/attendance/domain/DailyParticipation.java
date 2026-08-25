package com.memeboo2.haemi.elder.attendance.domain;

import com.memeboo2.haemi.common.attendance.ActivityType;
import com.memeboo2.haemi.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.util.UUID;

/** 어르신의 일별 참여 기록. 출석의 유일한 원천이다. */
@Entity
@Table(
        name = "elder_attendance_daily_participations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_daily_participation_elder_date",
                columnNames = {"elder_id", "participation_date"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyParticipation extends BaseEntity {

    @Column(name = "elder_id", nullable = false, columnDefinition = "uuid")
    private UUID elderId;

    @Column(name = "participation_date", nullable = false)
    private LocalDate participationDate;

    // 활동 종류별 완료 플래그 — 같은 종류를 여러 번 해도 true 하나로 집계 (횟수 아님).
    @Column(name = "training_done", nullable = false)
    @ColumnDefault("false")
    private boolean trainingDone;

    @Column(name = "greeting_read_done", nullable = false)
    @ColumnDefault("false")
    private boolean greetingReadDone;

    @Column(name = "memory_viewed_done", nullable = false)
    @ColumnDefault("false")
    private boolean memoryViewedDone;

    @Column(name = "replied_done", nullable = false)
    @ColumnDefault("false")
    private boolean repliedDone;

    public static DailyParticipation of(UUID elderId, LocalDate participationDate) {
        DailyParticipation p = new DailyParticipation();
        p.elderId = elderId;
        p.participationDate = participationDate;
        return p;
    }

    /**
     * 해당 활동 종류를 완료로 표시한다. 이미 켜져 있으면 false를 반환해
     * (어르신, 날짜, 종류)당 한 번만 이벤트가 발행되도록 한다 (멱등).
     */
    public boolean mark(ActivityType type) {
        switch (type) {
            case TRAINING -> {
                if (trainingDone) return false;
                trainingDone = true;
            }
            case GREETING_READ -> {
                if (greetingReadDone) return false;
                greetingReadDone = true;
            }
            case MEMORY_VIEWED -> {
                if (memoryViewedDone) return false;
                memoryViewedDone = true;
            }
            case REPLIED -> {
                if (repliedDone) return false;
                repliedDone = true;
            }
        }
        return true;
    }
}
