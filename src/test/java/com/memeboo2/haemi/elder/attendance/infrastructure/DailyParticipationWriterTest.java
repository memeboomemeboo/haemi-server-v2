package com.memeboo2.haemi.elder.attendance.infrastructure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DailyParticipationWriterTest {

    @Test
    void PostgreSQL에서는_동시_삽입에도_멱등한_ON_CONFLICT_구문을_선택한다() {
        String sql = DailyParticipationWriter.insertSqlFor("PostgreSQL");

        assertThat(sql).contains("ON CONFLICT (elder_id, participation_date) DO NOTHING");
        assertThat(sql).doesNotContain("MERGE INTO");
    }

    @Test
    void H2_테스트_환경에서는_호환되는_MERGE_구문을_선택한다() {
        String sql = DailyParticipationWriter.insertSqlFor("H2");

        assertThat(sql).contains("MERGE INTO elder_attendance_daily_participations");
    }
}
