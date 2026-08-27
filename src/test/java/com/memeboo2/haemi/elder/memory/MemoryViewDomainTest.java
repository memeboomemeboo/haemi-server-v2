package com.memeboo2.haemi.elder.memory;

import com.memeboo2.haemi.elder.memory.domain.MemoryView;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MemoryViewDomainTest {

    @Test
    void of는_elderId_memoryId_firstViewedAt을_올바르게_채운다() {
        UUID elderId = UUID.randomUUID();
        UUID memoryId = UUID.randomUUID();
        Instant firstViewedAt = Instant.now();

        MemoryView view = MemoryView.of(elderId, memoryId, firstViewedAt);

        assertThat(view.getElderId()).isEqualTo(elderId);
        assertThat(view.getMemoryId()).isEqualTo(memoryId);
        assertThat(view.getFirstViewedAt()).isEqualTo(firstViewedAt);
    }
}
