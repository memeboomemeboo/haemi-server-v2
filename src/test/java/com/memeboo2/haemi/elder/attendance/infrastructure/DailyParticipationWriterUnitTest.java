package com.memeboo2.haemi.elder.attendance.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.memeboo2.haemi.common.time.HaemiClock;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** DailyParticipationWriter가 JdbcTemplate/NamedParameterJdbcTemplate과 상호작용하는 방식을 검증한다. */
@ExtendWith(MockitoExtension.class)
class DailyParticipationWriterUnitTest {

    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private HaemiClock clock;

    private DailyParticipationWriter writer;

    private final Instant now = Instant.parse("2026-08-27T00:00:00Z");

    @BeforeEach
    void setUp() {
        writer = new DailyParticipationWriter(namedParameterJdbcTemplate, clock);
        org.mockito.Mockito.lenient().when(clock.now()).thenReturn(now);
    }

    @Test
    void PostgreSQL_환경에서_신규_삽입에_성공하면_true를_반환한다() throws Exception {
        stubDatabaseProductName("PostgreSQL");
        when(namedParameterJdbcTemplate.update(anyString(), anyMap())).thenReturn(1);

        boolean inserted = writer.insertIfAbsent(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now());

        assertThat(inserted).isTrue();
    }

    @Test
    void 이미_존재하는_행이면_false를_반환한다() throws Exception {
        stubDatabaseProductName("H2");
        when(namedParameterJdbcTemplate.update(anyString(), anyMap())).thenReturn(0);

        boolean inserted = writer.insertIfAbsent(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now());

        assertThat(inserted).isFalse();
    }

    @Test
    void 파라미터를_id_elderId_participationDate로_바인딩한다() throws Exception {
        stubDatabaseProductName("H2");
        when(namedParameterJdbcTemplate.update(anyString(), anyMap())).thenReturn(1);
        UUID id = UUID.randomUUID();
        UUID elderId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 8, 27);

        writer.insertIfAbsent(id, elderId, date);

        verify(namedParameterJdbcTemplate).update(anyString(),
                eq(Map.of("id", id, "elderId", elderId, "participationDate", date,
                        "now", Timestamp.from(now))));
    }

    @Test
    void DB_메타데이터는_여러_번_삽입해도_한_번만_조회한다() throws Exception {
        stubDatabaseProductName("PostgreSQL");
        when(namedParameterJdbcTemplate.update(anyString(), anyMap())).thenReturn(1);

        writer.insertIfAbsent(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now());
        writer.insertIfAbsent(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now());
        writer.insertIfAbsent(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now());

        // 방언 판별용 메타데이터 조회(execute)는 최초 1회만 수행되고 이후 캐시된 SQL을 재사용한다. (#140)
        verify(jdbcTemplate, org.mockito.Mockito.times(1)).execute(any(ConnectionCallback.class));
        verify(namedParameterJdbcTemplate, org.mockito.Mockito.times(3)).update(anyString(), anyMap());
    }

    private void stubDatabaseProductName(String productName) throws Exception {
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(metaData.getDatabaseProductName()).thenReturn(productName);
        when(connection.getMetaData()).thenReturn(metaData);

        when(namedParameterJdbcTemplate.getJdbcTemplate()).thenReturn(jdbcTemplate);
        when(jdbcTemplate.execute(any(ConnectionCallback.class)))
                .thenAnswer(invocation -> {
                    ConnectionCallback<String> callback = invocation.getArgument(0);
                    return callback.doInConnection(connection);
                });
    }
}
