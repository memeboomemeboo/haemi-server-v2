package com.memeboo2.haemi.guardian.memory;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.memory.application.GetMemoriesUseCase;
import com.memeboo2.haemi.guardian.memory.application.MemoryCreatorResolver;
import com.memeboo2.haemi.guardian.memory.application.MemoryWithCreator;
import com.memeboo2.haemi.guardian.memory.domain.Memory;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class GetMemoriesUseCaseTest {

    @Mock CareAccessQuery careAccessQuery;
    @Mock MemoryRepository memoryRepository;
    @Mock MemoryCreatorResolver creatorResolver;
    @Mock HaemiClock clock;
    @InjectMocks GetMemoriesUseCase useCase;

    UUID guardianId = UUID.randomUUID();
    UUID elderId = UUID.randomUUID();

    @Test
    void 정상_경로() {
        Instant now = Instant.parse("2026-08-25T00:00:00Z");
        given(clock.now()).willReturn(now);
        Memory memory = Memory.create(elderId, "제목", null, "한마디", 2020);
        given(memoryRepository.findByElderIdSince(elderId, now.minusSeconds(365L * 24 * 3600)))
                .willReturn(List.of(memory));
        MemoryWithCreator withCreator = new MemoryWithCreator(memory, "황정빈", null, true);
        given(creatorResolver.resolveAll(List.of(memory), elderId, guardianId)).willReturn(List.of(withCreator));

        List<MemoryWithCreator> result = useCase.execute(guardianId, elderId);

        assertThat(result).containsExactly(withCreator);
    }

    @Test
    void 링크없는_보호자는_403() {
        willThrow(new DomainException(ErrorCode.CARE_ACCESS_DENIED))
                .given(careAccessQuery).requireGuardianOf(guardianId, elderId);

        assertThatThrownBy(() -> useCase.execute(guardianId, elderId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CARE_ACCESS_DENIED));
    }
}
