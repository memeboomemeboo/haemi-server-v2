package com.memeboo2.haemi.elder.response.infrastructure;

import com.memeboo2.haemi.elder.response.domain.Response;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ResponseRepository extends JpaRepository<Response, UUID> {
    List<Response> findByMemoryId(UUID memoryId);

    List<Response> findByMemoryIdAndElderId(UUID memoryId, UUID elderId);

    List<Response> findByElderIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            UUID elderId, Instant from, Instant to);
}
