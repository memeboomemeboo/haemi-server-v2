package com.memeboo2.haemi.architecture;

import com.memeboo2.haemi.HaemiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModuleVerificationTest {

    @Test
    void 모듈_경계를_지킨다() {
        ApplicationModules.of(HaemiApplication.class).verify();
    }
}
