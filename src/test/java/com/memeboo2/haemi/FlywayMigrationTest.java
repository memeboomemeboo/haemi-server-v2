package com.memeboo2.haemi;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.sql.DriverManager;

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
        MigrationInfo[] migrations = flyway.info().all();
        assertThat(migrations).isNotEmpty();
        assertThat(migrations)
                .extracting(migration -> migration.getVersion().getVersion())
                .doesNotHaveDuplicates();
    }

    @Test
    void V133은_이벤트가_없는_기존_미전사_음성을_FAILED로_백필한다() throws Exception {
        String url = "jdbc:h2:mem:flyway-v133-transcript-status;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        String migration = java.nio.file.Files.readString(
                Path.of("src/main/resources/db/migration/V133__elder_response_transcript_status.sql"));

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE elder_responses (response_type VARCHAR(20) NOT NULL, transcript VARCHAR(1000))");
            statement.execute("INSERT INTO elder_responses (response_type, transcript) VALUES "
                    + "('VOICE', NULL), ('VOICE', '기존 전사'), ('TEXT', NULL)");
            for (String sql : migration.split(";")) {
                if (!sql.isBlank()) {
                    statement.execute(sql);
                }
            }

            try (var result = statement.executeQuery(
                    "SELECT transcript_status FROM elder_responses WHERE response_type = 'VOICE' AND transcript IS NULL")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isEqualTo("FAILED");
            }
            try (var result = statement.executeQuery(
                    "SELECT transcript_status FROM elder_responses WHERE response_type = 'VOICE' AND transcript IS NOT NULL")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isEqualTo("COMPLETED");
            }
        }
    }
}
