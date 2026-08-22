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

    @OneToMany(mappedBy = "family", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FamilyMember> members = new ArrayList<>();

    public static Family create(String name) {
        Family family = new Family();
        family.name = name;
        return family;
    }

    public void addMember(UUID userId) {
        members.add(FamilyMember.of(this, userId));
    }

    public boolean hasMember(UUID userId) {
        return members.stream().anyMatch(m -> m.getUserId().equals(userId));
    }

    public long guardianCount() {
        return members.size();
    }

    public long elderCount() {
        return members.stream().filter(FamilyMember::isElder).count();
    }
}
