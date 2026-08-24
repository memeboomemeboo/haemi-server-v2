package com.memeboo2.haemi.guardian.memory.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.memory.domain.Memory;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteMemoryUseCase {

    private final MemoryRepository memoryRepository;
    private final HaemiClock clock;

    @Transactional
    public void execute(UUID guardianId, UUID memoryId) {
        Memory memory = memoryRepository.findById(memoryId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND));

        // R5: 생성자 본인만 삭제 가능
        if (!guardianId.equals(memory.getCreatedBy())) {
            throw new DomainException(ErrorCode.NOT_RESOURCE_OWNER);
        }

        memory.delete(clock.now());
    }
}
