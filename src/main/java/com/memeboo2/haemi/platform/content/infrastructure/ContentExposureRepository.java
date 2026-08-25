package com.memeboo2.haemi.platform.content.infrastructure;

import com.memeboo2.haemi.platform.content.domain.ContentExposure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ContentExposureRepository extends JpaRepository<ContentExposure, UUID> {

    @Query("""
            SELECT e.contentId FROM ContentExposure e
            WHERE e.elderId = :elderId AND e.exposedAt >= :since
            """)
    List<UUID> findContentIdsExposedSince(UUID elderId, Instant since);

    List<ContentExposure> findByElderId(UUID elderId);
}
