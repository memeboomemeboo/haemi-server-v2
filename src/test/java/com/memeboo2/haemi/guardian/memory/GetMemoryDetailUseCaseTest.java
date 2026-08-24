package com.memeboo2.haemi.guardian.memory;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.memory.application.GetMemoryDetailUseCase;
import com.memeboo2.haemi.guardian.memory.application.MemoryCreatorResolver;
import com.memeboo2.haemi.guardian.memory.application.MemoryWithCreator;
import com.memeboo2.haemi.guardian.memory.domain.Memory;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class GetMemoryDetailUseCaseTest {

    @Mock CareAccessQuery careAccessQuery;
    @Mock MemoryRepository memoryRepository;
    @Mock MemoryCreatorResolver creatorResolver;
    @InjectMocks GetMemoryDetailUseCase useCase;

    UUID guardianId = UUID.randomUUID();
    UUID elderId = UUID.randomUUID();
    UUID memoryId = UUID.randomUUID();

    @Test
    void 정상_경로() {
        Memory memory = Memory.create(elderId, "제목", null, "한마디", 2020);
        given(memoryRepository.findByIdWithImages(memoryId)).willReturn(Optional.of(memory));
        MemoryWithCreator withCreator = new MemoryWithCreator(memory, "황정빈", null, true);
        given(creatorResolver.resolve(memory, guardianId)).willReturn(withCreator);

        MemoryWithCreator result = useCase.execute(guardianId, memoryId);

        assertThat(result).isEqualTo(withCreator);
    }

    @Test
    void 링크없는_보호자는_403() {
        Memory memory = Memory.create(elderId, "제목", null, "한마디", 2020);
        given(memoryRepository.findByIdWithImages(memoryId)).willReturn(Optional.of(memory));
        willThrow(new DomainException(ErrorCode.CARE_ACCESS_DENIED))
                .given(careAccessQuery).requireGuardianOf(guardianId, elderId);

        assertThatThrownBy(() -> useCase.execute(guardianId, memoryId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CARE_ACCESS_DENIED));
    }
}
