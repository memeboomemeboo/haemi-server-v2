package com.memeboo2.haemi.guardian.eldermanagement.domain;

import com.memeboo2.haemi.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "guardian_elders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Elder extends BaseEntity {

    /** auth_users.id 참조 (FK 없음 — 모듈 간 FK 금지) */
    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(nullable = false, length = 30)
    private String name;

    @Column
    private LocalDate birthDate;

    @Column(nullable = false, columnDefinition = "uuid")
    private UUID familyId;

    public static Elder create(UUID userId, UUID familyId, String name, LocalDate birthDate) {
        Elder e = new Elder();
        e.userId = userId;
        e.familyId = familyId;
        e.name = name;
        e.birthDate = birthDate;
        return e;
    }
}
