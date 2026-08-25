package com.memeboo2.haemi.elder.memory.domain;

import com.memeboo2.haemi.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** 어르신이 추억을 처음 열어본 사실. (elderId, memoryId) 당 1행. */
@Entity
@Table(
        name = "elder_memory_views",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_memory_view_elder_memory",
                columnNames = {"elder_id", "memory_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemoryView extends BaseEntity {

    @Column(name = "elder_id", nullable = false, columnDefinition = "uuid")
    private UUID elderId;

    @Column(name = "memory_id", nullable = false, columnDefinition = "uuid")
    private UUID memoryId;

    @Column(name = "first_viewed_at", nullable = false)
    private Instant firstViewedAt;

    public static MemoryView of(UUID elderId, UUID memoryId, Instant firstViewedAt) {
        MemoryView v = new MemoryView();
        v.elderId = elderId;
        v.memoryId = memoryId;
        v.firstViewedAt = firstViewedAt;
        return v;
    }
}
