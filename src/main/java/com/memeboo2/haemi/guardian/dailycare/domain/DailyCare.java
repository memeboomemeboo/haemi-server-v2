package com.memeboo2.haemi.guardian.dailycare.domain;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(
    name = "guardian_daily_cares",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_daily_care_guardian_elder_date",
        columnNames = {"guardian_id", "elder_id", "care_date"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyCare extends BaseEntity {

    private static final int MAX_TEXT_LENGTH = 100;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Column(nullable = false)
    private UUID guardianId;

    @Column(nullable = false)
    private UUID elderId;

    @Column(nullable = false)
    private LocalDate careDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CareType careType;

    @Column(length = 100)
    private String text;

    /** 음성 미디어 storageKey */
    @Column(length = 500)
    private String mediaKey;

    /** 음성 길이(초). 텍스트 타입이면 null. */
    @Column
    private Integer durationSeconds;

    @Column(nullable = false)
    private Instant retainUntil;

    @Column
    private Instant viewedAt;

    public static DailyCare text(UUID guardianId, UUID elderId, LocalDate careDate,
                                 String text, int retentionDays) {
        if (text == null || text.isBlank()) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "텍스트를 입력해주세요.");
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "텍스트는 " + MAX_TEXT_LENGTH + "자 이하입니다.");
        }
        DailyCare c = new DailyCare();
        c.guardianId = guardianId;
        c.elderId = elderId;
        c.careDate = careDate;
        c.careType = CareType.TEXT;
        c.text = text;
        c.retainUntil = careDate.plusDays(retentionDays).atStartOfDay()
                .atZone(KST).toInstant();
        return c;
    }

    public static DailyCare voice(UUID guardianId, UUID elderId, LocalDate careDate,
                                  String mediaKey, int durationSeconds, int retentionDays) {
        DailyCare c = new DailyCare();
        c.guardianId = guardianId;
        c.elderId = elderId;
        c.careDate = careDate;
        c.careType = CareType.VOICE;
        c.mediaKey = mediaKey;
        c.durationSeconds = durationSeconds;
        c.retainUntil = careDate.plusDays(retentionDays).atStartOfDay()
                .atZone(KST).toInstant();
        return c;
    }

    public void markViewed(Instant now) {
        if (this.viewedAt == null) {
            this.viewedAt = now;
        }
    }

    public boolean isRead() {
        return viewedAt != null;
    }
}
