package com.memeboo2.haemi.common.event;

import org.springframework.modulith.events.Externalized;

import java.time.LocalDate;
import java.util.UUID;

@Externalized
public record GreetingSent(UUID dailyCareId, UUID guardianId, UUID elderId, LocalDate careDate) {}
