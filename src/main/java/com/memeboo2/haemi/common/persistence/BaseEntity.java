package com.memeboo2.haemi.common.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(updatable = false)
    private UUID createdBy;

    @Column
    private Instant deletedAt;

    @PrePersist
    protected void prePersist() {
        assignIdIfAbsent();
    }

    /**
     * 영속화 전에도 식별자가 필요한 도메인 객체가 사용할 수 있는 UUID 초기화 지점이다.
     */
    protected void assignIdIfAbsent() {
        if (id == null) {
            id = UuidGenerator.generate();
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    protected void softDelete(Instant now) {
        this.deletedAt = now;
    }
}
