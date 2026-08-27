package com.memeboo2.haemi.guardian.eldermanagement;

import com.memeboo2.haemi.guardian.eldermanagement.domain.Elder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ElderDomainTest {

    @Test
    void create로_어르신을_생성한다() {
        UUID userId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        LocalDate birth = LocalDate.of(1945, 3, 15);

        Elder elder = Elder.create(userId, familyId, "홍길동", birth);

        assertThat(elder.getUserId()).isEqualTo(userId);
        assertThat(elder.getFamilyId()).isEqualTo(familyId);
        assertThat(elder.getName()).isEqualTo("홍길동");
        assertThat(elder.getBirthDate()).isEqualTo(birth);
    }

    @Test
    void birthDate가_null이어도_생성_가능하다() {
        Elder elder = Elder.create(UUID.randomUUID(), UUID.randomUUID(), "이름", null);

        assertThat(elder.getBirthDate()).isNull();
        assertThat(elder.getName()).isEqualTo("이름");
    }
}
