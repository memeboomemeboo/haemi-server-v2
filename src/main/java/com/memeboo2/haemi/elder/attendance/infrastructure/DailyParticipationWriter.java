package com.memeboo2.haemi.elder.attendance.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/** 일별 출석을 한 번만 적재한다. PostgreSQL과 H2의 upsert 문법 차이를 이곳에 국한한다. */
@Component
@RequiredArgsConstructor
public class DailyParticipationWriter {

    private static final String POSTGRESQL_INSERT_IF_ABSENT = """
            INSERT INTO elder_attendance_daily_participations
                    (id, elder_id, participation_date, created_at, updated_at)
            VALUES (:id, :elderId, :participationDate, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (elder_id, participation_date) DO NOTHING
            """;

    private static final String H2_INSERT_IF_ABSENT = """
            MERGE INTO elder_attendance_daily_participations AS target
            USING (VALUES (:id, :elderId, :participationDate)) AS source(id, elder_id, participation_date)
            ON target.elder_id = source.elder_id AND target.participation_date = source.participation_date
            WHEN NOT MATCHED THEN
                INSERT (id, elder_id, participation_date, created_at, updated_at)
                VALUES (source.id, source.elder_id, source.participation_date, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /** DB 제품명은 애플리케이션 수명 동안 바뀌지 않으므로, 결정된 SQL을 한 번만 계산해 캐시한다. */
    private volatile String insertSql;

    /**
     * PostgreSQL은 ON CONFLICT가 동시 INSERT에서도 한 건만 성공시키는 멱등 경로다.
     * H2는 해당 문법을 지원하지 않아 테스트 프로필에서만 MERGE를 사용한다.
     */
    public boolean insertIfAbsent(UUID id, UUID elderId, LocalDate participationDate) {
        return jdbcTemplate.update(resolvedInsertSql(), Map.of(
                "id", id,
                "elderId", elderId,
                "participationDate", participationDate)) == 1;
    }

    /**
     * 첫 호출에서만 커넥션 메타데이터로 방언을 판별하고 이후에는 캐시된 SQL을 재사용한다.
     * 두 스레드가 동시에 계산해도 결과가 동일하므로 별도 잠금 없이 volatile 가시성만으로 충분하다.
     */
    private String resolvedInsertSql() {
        String sql = insertSql;
        if (sql == null) {
            sql = jdbcTemplate.getJdbcTemplate().execute(
                    (ConnectionCallback<String>) connection -> insertSqlFor(connection.getMetaData().getDatabaseProductName()));
            insertSql = sql;
        }
        return sql;
    }

    static String insertSqlFor(String databaseProductName) {
        return "PostgreSQL".equals(databaseProductName)
                ? POSTGRESQL_INSERT_IF_ABSENT
                : H2_INSERT_IF_ABSENT;
    }
}
