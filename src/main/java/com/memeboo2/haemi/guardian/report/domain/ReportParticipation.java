package com.memeboo2.haemi.guardian.report.domain;

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

/**
 * guardian/report가 AttendanceRecorded를 소비해 적재하는 자체 스냅샷.
 * elder/attendance의 원천 테이블을 직접 조회하지 않는다.
 */
@Entity
@Table(
        name = "guardian_report_participations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_report_participation_elder_date",
                columnNames = {"elder_id", "participation_date"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportParticipation extends BaseEntity {

    @Column(name = "elder_id", nullable = false, columnDefinition = "uuid")
    private UUID elderId;

    @Column(name = "participation_date", nullable = false)
    private LocalDate participationDate;

    // 활동 종류별 완료 플래그 (attendance 스냅샷 미러링).
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

    public static ReportParticipation of(UUID elderId, LocalDate participationDate) {
        ReportParticipation p = new ReportParticipation();
        p.elderId = elderId;
        p.participationDate = participationDate;
        return p;
    }
}
