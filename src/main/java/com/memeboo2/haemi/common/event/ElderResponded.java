package com.memeboo2.haemi.common.event;

import org.springframework.modulith.events.Externalized;

import java.util.UUID;

@Externalized
public record ElderResponded(UUID memoryId, UUID elderId) {}
