package com.memeboo2.haemi.guardian.memory;

import com.memeboo2.haemi.guardian.memory.domain.MemoryRegistered;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MemoryRegisteredEventTest {

    @Test
    void 이벤트_레코드_필드를_확인한다() {
        UUID memoryId = UUID.randomUUID();
        UUID elderId = UUID.randomUUID();
        UUID guardianId = UUID.randomUUID();

        MemoryRegistered event = new MemoryRegistered(memoryId, elderId, guardianId);

        assertThat(event.memoryId()).isEqualTo(memoryId);
        assertThat(event.elderId()).isEqualTo(elderId);
        assertThat(event.guardianId()).isEqualTo(guardianId);
    }

    @Test
    void 같은_값이면_equals가_true이다() {
        UUID m = UUID.randomUUID(), e = UUID.randomUUID(), g = UUID.randomUUID();
        assertThat(new MemoryRegistered(m, e, g)).isEqualTo(new MemoryRegistered(m, e, g));
    }
}
