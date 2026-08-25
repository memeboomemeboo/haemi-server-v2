package com.memeboo2.haemi.platform.content.domain;

import com.memeboo2.haemi.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "platform_content_exposures")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentExposure extends BaseEntity {

    @Column(nullable = false)
    private UUID elderId;

    @Column(nullable = false)
    private UUID contentId;

    @Column(nullable = false)
    private Instant exposedAt;

    public static ContentExposure record(UUID elderId, UUID contentId, Instant exposedAt) {
        ContentExposure exposure = new ContentExposure();
        exposure.assignIdIfAbsent();
        exposure.elderId = elderId;
        exposure.contentId = contentId;
        exposure.exposedAt = exposedAt;
        return exposure;
    }
}
