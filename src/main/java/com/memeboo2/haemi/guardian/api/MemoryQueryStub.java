package com.memeboo2.haemi.guardian.api;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/** guardian/memory 실구현 전까지 사용하는 빈 구현체. */
@Component
@ConditionalOnMissingBean(name = "memoryQueryImpl")
public class MemoryQueryStub implements MemoryQuery {

    @Override
    public List<MemoryMaterial> materialsFor(UUID elderId, int limit) {
        return List.of();
    }
}
