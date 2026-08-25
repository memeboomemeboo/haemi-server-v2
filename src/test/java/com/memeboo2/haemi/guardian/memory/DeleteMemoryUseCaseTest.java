package com.memeboo2.haemi.guardian.memory;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.memory.application.DeleteMemoryUseCase;
import com.memeboo2.haemi.guardian.memory.domain.Memory;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DeleteMemoryUseCaseTest {

    @Mock MemoryRepository memoryRepository;
    @Mock HaemiClock clock;
    @InjectMocks DeleteMemoryUseCase useCase;

    @Test
    void 이미_삭제된_추억은_다시_삭제할_수_없다_404() {
        UUID guardianId = UUID.randomUUID();
        UUID memoryId = UUID.randomUUID();
        Memory memory = Memory.create(UUID.randomUUID(), "제목", "메모", "한마디", 2020);
        memory.delete(Instant.parse("2026-08-24T00:00:00Z")); // 이미 소프트 삭제됨
        given(memoryRepository.findById(memoryId)).willReturn(Optional.of(memory));

        assertThatThrownBy(() -> useCase.execute(guardianId, memoryId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }
}
