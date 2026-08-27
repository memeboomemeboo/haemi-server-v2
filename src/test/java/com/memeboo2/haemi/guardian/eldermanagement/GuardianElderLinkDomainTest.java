package com.memeboo2.haemi.guardian.eldermanagement;

import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLink;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GuardianElderLinkDomainTest {

    @Test
    void create로_링크를_생성하면_기본_역할은_GUARDIAN이다() {
        UUID guardianId = UUID.randomUUID();
        UUID elderId = UUID.randomUUID();

        GuardianElderLink link = GuardianElderLink.create(guardianId, elderId);

        assertThat(link.getGuardianId()).isEqualTo(guardianId);
        assertThat(link.getElderId()).isEqualTo(elderId);
        assertThat(link.getRole()).isEqualTo(GuardianRole.GUARDIAN);
        assertThat(link.getLinkedAt()).isNotNull();
    }

    @Test
    void changeRole로_역할을_변경한다() {
        GuardianElderLink link = GuardianElderLink.create(UUID.randomUUID(), UUID.randomUUID());

        link.changeRole(GuardianRole.OTHER);

        assertThat(link.getRole()).isEqualTo(GuardianRole.OTHER);
    }

    @Test
    void changeRole을_여러번_호출하면_마지막_역할이_적용된다() {
        GuardianElderLink link = GuardianElderLink.create(UUID.randomUUID(), UUID.randomUUID());

        link.changeRole(GuardianRole.OTHER);
        link.changeRole(GuardianRole.GUARDIAN);

        assertThat(link.getRole()).isEqualTo(GuardianRole.GUARDIAN);
    }

    @Test
    void 서로_다른_링크는_다른_linkedAt을_가질_수_있다() {
        GuardianElderLink link1 = GuardianElderLink.create(UUID.randomUUID(), UUID.randomUUID());
        GuardianElderLink link2 = GuardianElderLink.create(UUID.randomUUID(), UUID.randomUUID());

        assertThat(link1.getLinkedAt()).isNotNull();
        assertThat(link2.getLinkedAt()).isNotNull();
    }
}
