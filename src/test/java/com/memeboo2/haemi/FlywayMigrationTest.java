package com.memeboo2.haemi;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** 신규 DB에서 baseline과 phase 마이그레이션이 중복되지 않도록 회귀를 막는다. */
class FlywayMigrationTest {

    @Test
    void baseline_파일_없이_전체_migration_체인이_발견된다() {
        Flyway flyway = Flyway.configure()
                .dataSource("jdbc:h2:mem:flyway-migration-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;"
                        + "INIT=CREATE DOMAIN IF NOT EXISTS TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE", "sa", "")
                .locations("classpath:db/migration")
                .load();

        assertThat(Path.of("src/main/resources/db/migration/V1__baseline.sql")).doesNotExist();
        assertThat(flyway.info().all()).hasSize(12);
    }
}
