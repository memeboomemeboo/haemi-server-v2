package com.memeboo2.haemi.elder.memory.infrastructure;

import com.memeboo2.haemi.elder.memory.domain.MemoryView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface MemoryViewRepository extends JpaRepository<MemoryView, UUID> {

    /**
     * 존재하면 아무것도 하지 않는 원자적 삽입.
     * 반환값이 1이면 최초 열람으로 새로 적재된 것 — 그때만 MemoryViewed를 발행한다.
     * exists-then-insert는 동시 요청이 unique 위반을 일으키므로 ON CONFLICT로 멱등 처리한다.
     */
    @Modifying
    @Query(value = """
            INSERT INTO elder_memory_views (elder_id, memory_id, first_viewed_at)
            VALUES (:elderId, :memoryId, :firstViewedAt)
            ON CONFLICT (elder_id, memory_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("elderId") UUID elderId,
                       @Param("memoryId") UUID memoryId,
                       @Param("firstViewedAt") Instant firstViewedAt);
}
