package com.memeboo2.haemi.guardian.report.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** ReportParticipationWriter의 DB 방언별 upsert SQL 선택 및 삽입 결과 반환 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class ReportParticipationWriterTest {

    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private ReportParticipationWriter writer;

    @BeforeEach
    void setUp() {
        writer = new ReportParticipationWriter(namedParameterJdbcTemplate);
    }

    @Test
    void PostgreSQL_방언에서는_ON_CONFLICT_구문을_사용한다() {
        String sql = ReportParticipationWriter.insertSqlFor("PostgreSQL");

        assertThat(sql).contains("ON CONFLICT (elder_id, participation_date) DO NOTHING");
        assertThat(sql).contains("guardian_report_participations");
    }

    @Test
    void H2_방언에서는_MERGE_구문을_사용한다() {
        String sql = ReportParticipationWriter.insertSqlFor("H2");

        assertThat(sql).contains("MERGE INTO guardian_report_participations");
    }

    @Test
    void 알수없는_방언은_H2_구문으로_대체된다() {
        String sql = ReportParticipationWriter.insertSqlFor("Oracle");

        assertThat(sql).contains("MERGE INTO");
    }

    @Test
    void 신규_삽입에_성공하면_true를_반환한다() throws Exception {
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(metaData.getDatabaseProductName()).thenReturn("H2");
        when(connection.getMetaData()).thenReturn(metaData);

        when(namedParameterJdbcTemplate.getJdbcTemplate()).thenReturn(jdbcTemplate);
        when(jdbcTemplate.execute(any(org.springframework.jdbc.core.ConnectionCallback.class)))
                .thenAnswer(invocation -> {
                    org.springframework.jdbc.core.ConnectionCallback<String> callback = invocation.getArgument(0);
                    return callback.doInConnection(connection);
                });
        when(namedParameterJdbcTemplate.update(anyString(), anyMap())).thenReturn(1);

        UUID id = UUID.randomUUID();
        UUID elderId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 8, 27);

        boolean inserted = writer.insertIfAbsent(id, elderId, date);

        assertThat(inserted).isTrue();
        verify(namedParameterJdbcTemplate).update(anyString(),
                org.mockito.ArgumentMatchers.eq(Map.of("id", id, "elderId", elderId, "participationDate", date)));
    }

    @Test
    void 이미_존재하는_행이면_false를_반환한다() throws Exception {
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(connection.getMetaData()).thenReturn(metaData);

        when(namedParameterJdbcTemplate.getJdbcTemplate()).thenReturn(jdbcTemplate);
        when(jdbcTemplate.execute(any(org.springframework.jdbc.core.ConnectionCallback.class)))
                .thenAnswer(invocation -> {
                    org.springframework.jdbc.core.ConnectionCallback<String> callback = invocation.getArgument(0);
                    return callback.doInConnection(connection);
                });
        when(namedParameterJdbcTemplate.update(anyString(), anyMap())).thenReturn(0);

        boolean inserted = writer.insertIfAbsent(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now());

        assertThat(inserted).isFalse();
    }
}
