package com.memeboo2.haemi.common.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

// soft delete 필드를 베이스에 두는 이상, 필터도 베이스에서 강제한다.
// 서브클래스가 조회 쿼리마다 deleted_at IS NULL을 직접 붙이는 걸 잊어도 소프트 삭제 행이 새지 않도록,
// @MappedSuperclass에 선언한 @SQLRestriction이 모든 엔티티 로딩에 적용된다.
@Getter
@MappedSuperclass
@SQLRestriction("deleted_at IS NULL")
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity implements Persistable<UUID> {

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

    // JPA가 @PostLoad/@PostPersist로 관리하는 필드 — DB에 저장하지 않음.
    @Transient
    private boolean isNew = true;

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

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
