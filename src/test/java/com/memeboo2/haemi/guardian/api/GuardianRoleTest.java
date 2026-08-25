package com.memeboo2.haemi.guardian.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GuardianRoleTest {

    @Test
    void 코드값과_표시명이_분리되어_있다() {
        assertThat(GuardianRole.DAUGHTER.name()).isEqualTo("DAUGHTER");
        assertThat(GuardianRole.DAUGHTER.getLabel()).isEqualTo("딸");
        assertThat(GuardianRole.OTHER.getLabel()).isEqualTo("기타");
    }
}
