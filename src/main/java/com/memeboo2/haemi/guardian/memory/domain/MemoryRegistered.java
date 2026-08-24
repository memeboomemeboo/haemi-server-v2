package com.memeboo2.haemi.guardian.memory.domain;

import org.springframework.modulith.events.Externalized;

import java.util.UUID;

@Externalized
public record MemoryRegistered(UUID memoryId, UUID elderId, UUID guardianId) {}
