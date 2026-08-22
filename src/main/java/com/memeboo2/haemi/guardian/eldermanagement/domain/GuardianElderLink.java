package com.memeboo2.haemi.guardian.eldermanagement.domain;

import com.memeboo2.haemi.common.persistence.BaseEntity;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "guardian_elder_links",
       uniqueConstraints = @UniqueConstraint(columnNames = {"guardian_id", "elder_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuardianElderLink extends BaseEntity {

    @Column(name = "guardian_id", nullable = false, columnDefinition = "uuid")
    private UUID guardianId;

    @Column(name = "elder_id", nullable = false, columnDefinition = "uuid")
    private UUID elderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GuardianRole role;

    @Column(nullable = false)
    private Instant linkedAt;

    public static GuardianElderLink create(UUID guardianId, UUID elderId) {
        GuardianElderLink link = new GuardianElderLink();
        link.guardianId = guardianId;
        link.elderId = elderId;
        link.role = GuardianRole.보호자;
        link.linkedAt = Instant.now();
        return link;
    }

    public void changeRole(GuardianRole role) {
        this.role = role;
    }
}
