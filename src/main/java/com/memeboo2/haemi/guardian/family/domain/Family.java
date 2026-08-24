package com.memeboo2.haemi.guardian.family.domain;

import com.memeboo2.haemi.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "guardian_families")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Family extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 30)
    private String memo;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @OneToMany(mappedBy = "family", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FamilyMember> members = new ArrayList<>();

    public static Family create(String name, String memo, String profileImageUrl) {
        Family family = new Family();
        family.name = name;
        family.memo = memo;
        family.profileImageUrl = profileImageUrl;
        return family;
    }

    public static Family create(String name) {
        return create(name, null, null);
    }

    public void addMember(UUID userId) {
        members.add(FamilyMember.of(this, userId));
    }

    public boolean hasMember(UUID userId) {
        return members.stream().anyMatch(m -> m.getUserId().equals(userId));
    }

    public long guardianCount() {
        return members.stream().filter(m -> !m.isElder()).count();
    }
}
