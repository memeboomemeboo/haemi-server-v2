package com.memeboo2.haemi.guardian.memory.application;

import com.memeboo2.haemi.elder.api.ElderResponded;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ElderRespondedListener {

    private final MemoryRepository memoryRepository;

    @ApplicationModuleListener
    public void on(ElderResponded event) {
        memoryRepository.findById(event.memoryId())
                .filter(m -> !m.isResponded())
                .ifPresent(m -> {
                    m.markResponded();
                    memoryRepository.save(m);
                });
    }
}
