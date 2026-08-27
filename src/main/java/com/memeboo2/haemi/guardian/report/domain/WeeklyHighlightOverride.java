package com.memeboo2.haemi.guardian.report.domain;

import com.memeboo2.haemi.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 보호자가 편집한 "이번 주 하이라이트" 문구 (#100 M5).
 * (elderId, weekStart) 단위로 자동 생성 문구를 덮어쓴다. weekStart는 해당 주의 월요일(ISO).
 */
@Entity
@Table(name = "weekly_highlight_overrides",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_weekly_highlight_elder_week", columnNames = {"elder_id", "week_start"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklyHighlightOverride extends BaseEntity {

    @Column(name = "elder_id", nullable = false)
    private UUID elderId;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    /** 편집된 문구. 줄바꿈(\n)으로 여러 줄을 구분한다. */
    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    public static WeeklyHighlightOverride of(UUID elderId, LocalDate weekStart, String content) {
        WeeklyHighlightOverride o = new WeeklyHighlightOverride();
        o.elderId = elderId;
        o.weekStart = weekStart;
        o.content = content;
        return o;
    }

    public void updateContent(String content) {
        this.content = content;
    }
}
