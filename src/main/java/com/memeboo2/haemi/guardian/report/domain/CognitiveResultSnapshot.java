package com.memeboo2.haemi.guardian.report.domain;

import com.memeboo2.haemi.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/** guardian/report가 CIST 완료 이벤트에서 적재한 영역별 읽기 모델이다. */
@Entity
@Table(
        name = "guardian_report_cognitive_results",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_report_cognitive_result_session_area",
                columnNames = {"session_id", "cognitive_area"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CognitiveResultSnapshot extends BaseEntity {

    @Column(name = "elder_id", nullable = false, columnDefinition = "uuid")
    private UUID elderId;

    @Column(name = "session_id", nullable = false, columnDefinition = "uuid")
    private UUID sessionId;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "cognitive_area", nullable = false, length = 20)
    private String cognitiveArea;

    @Column(name = "scored_answer_count", nullable = false)
    private int scoredAnswerCount;

    @Column(name = "correct_answer_count", nullable = false)
    private int correctAnswerCount;

    public static CognitiveResultSnapshot of(
            UUID elderId,
            UUID sessionId,
            LocalDate sessionDate,
            String cognitiveArea,
            int scoredAnswerCount,
            int correctAnswerCount
    ) {
        CognitiveResultSnapshot snapshot = new CognitiveResultSnapshot();
        snapshot.elderId = elderId;
        snapshot.sessionId = sessionId;
        snapshot.sessionDate = sessionDate;
        snapshot.cognitiveArea = cognitiveArea;
        snapshot.scoredAnswerCount = scoredAnswerCount;
        snapshot.correctAnswerCount = correctAnswerCount;
        return snapshot;
    }
}
