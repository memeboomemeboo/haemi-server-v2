package com.memeboo2.haemi.elder.reminiscence.infrastructure;

import com.memeboo2.haemi.elder.reminiscence.domain.GeneratedReminiscence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface GeneratedReminiscenceRepository extends JpaRepository<GeneratedReminiscence, UUID> {

    Optional<GeneratedReminiscence> findByElderIdAndContentDate(UUID elderId, LocalDate contentDate);
}
