package com.memeboo2.haemi.guardian.family.domain;

import com.memeboo2.haemi.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "guardian_family_members",
       uniqueConstraints = @UniqueConstraint(columnNames = {"family_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FamilyMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberType memberType;

    public enum MemberType { GUARDIAN, ELDER }

    static FamilyMember of(Family family, UUID userId) {
        FamilyMember m = new FamilyMember();
        m.family = family;
        m.userId = userId;
        m.memberType = MemberType.GUARDIAN;
        return m;
    }

    public boolean isElder() {
        return memberType == MemberType.ELDER;
    }
}
