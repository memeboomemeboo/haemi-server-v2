package com.memeboo2.haemi.platform.content.infrastructure;

import com.memeboo2.haemi.platform.content.domain.ContentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ContentItemRepository extends JpaRepository<ContentItem, UUID> {

    @Query("""
            SELECT c FROM ContentItem c
            WHERE c.region = :region
              AND (c.availableUntil IS NULL OR c.availableUntil > :now)
              AND (:age IS NULL OR c.recommendedMinAge IS NULL OR c.recommendedMinAge <= :age)
              AND (:age IS NULL OR c.recommendedMaxAge IS NULL OR c.recommendedMaxAge >= :age)
            ORDER BY c.createdAt ASC
            """)
    List<ContentItem> findEligible(String region, Integer age, Instant now);
}
