package com.memeboo2.haemi.guardian.memory.infrastructure;

import com.memeboo2.haemi.guardian.memory.domain.Memory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemoryRepository extends JpaRepository<Memory, UUID> {

    @Query("SELECT m FROM Memory m WHERE m.elderId = :elderId AND m.deletedAt IS NULL AND m.createdAt >= :since ORDER BY m.createdAt DESC")
    List<Memory> findByElderIdSince(UUID elderId, Instant since);

    @Query("SELECT m FROM Memory m LEFT JOIN FETCH m.images WHERE m.id = :id AND m.deletedAt IS NULL")
    Optional<Memory> findByIdWithImages(UUID id);
}
