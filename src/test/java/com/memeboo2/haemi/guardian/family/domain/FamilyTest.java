package com.memeboo2.haemi.guardian.family.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FamilyTest {

    @Test
    void guardianCount은_추가된_보호자_수를_반환한다() {
        Family family = Family.create("테스트 가족");
        family.addMember(UUID.randomUUID());
        family.addMember(UUID.randomUUID());

        assertThat(family.guardianCount()).isEqualTo(2);
    }

    @Test
    void 멤버가_없으면_guardianCount는_0이다() {
        Family family = Family.create("테스트 가족");

        assertThat(family.guardianCount()).isEqualTo(0);
    }
}
