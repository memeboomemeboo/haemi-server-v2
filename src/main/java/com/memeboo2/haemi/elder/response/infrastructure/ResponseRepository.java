package com.memeboo2.haemi.elder.response.infrastructure;

import com.memeboo2.haemi.elder.response.domain.Response;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ResponseRepository extends JpaRepository<Response, UUID> {

    @Query("SELECT DISTINCT r FROM Response r LEFT JOIN FETCH r.emotions WHERE r.memoryId = :memoryId")
    List<Response> findByMemoryId(UUID memoryId);

    @Query("SELECT DISTINCT r FROM Response r LEFT JOIN FETCH r.emotions WHERE r.memoryId = :memoryId AND r.elderId = :elderId")
    List<Response> findByMemoryIdAndElderId(UUID memoryId, UUID elderId);

    List<Response> findByElderIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            UUID elderId, Instant from, Instant to);
}
