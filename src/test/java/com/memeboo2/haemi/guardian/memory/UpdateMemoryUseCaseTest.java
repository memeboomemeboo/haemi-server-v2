package com.memeboo2.haemi.guardian.memory;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.persistence.BaseEntity;
import com.memeboo2.haemi.guardian.memory.application.UpdateMemoryUseCase;
import com.memeboo2.haemi.guardian.memory.domain.Memory;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
import com.memeboo2.haemi.platform.api.MediaPurpose;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UpdateMemoryUseCaseTest {

    @Mock MemoryRepository memoryRepository;
    @Mock MediaUploadCommand mediaUploadCommand;
    @InjectMocks UpdateMemoryUseCase useCase;

    UUID guardianId = UUID.randomUUID();
    UUID elderId = UUID.randomUUID();
    UUID memoryId = UUID.randomUUID();

    private void setCreatedBy(Memory memory, UUID createdBy) throws Exception {
        Field field = BaseEntity.class.getDeclaredField("createdBy");
        field.setAccessible(true);
        field.set(memory, createdBy);
    }

    @Test
    void 정상_경로_수정() throws Exception {
        Memory memory = Memory.create(elderId, "제목", "메모", "한마디", 2020);
        setCreatedBy(memory, guardianId);
        given(memoryRepository.findByIdWithImages(memoryId)).willReturn(Optional.of(memory));

        UUID mediaRefId = UUID.randomUUID();
        given(mediaUploadCommand.memoryImageMaxCount()).willReturn(4);
        given(mediaUploadCommand.confirmUpload(guardianId, mediaRefId, MediaPurpose.MEMORY_IMAGE))
                .willReturn(URI.create("https://image.example/new.png"));

        useCase.execute(guardianId, memoryId, "새제목", "새메모", "새한마디", 2021, 5, "대구",
                List.of(mediaRefId));

        assertThat(memory.getTitle()).isEqualTo("새제목");
        assertThat(memory.getMemo()).isEqualTo("새메모");
        assertThat(memory.getMessage()).isEqualTo("새한마디");
        assertThat(memory.getMemoryYear()).isEqualTo(2021);
        assertThat(memory.getMemoryMonth()).isEqualTo(5);
        assertThat(memory.getPlace()).isEqualTo("대구");
    }

    @Test
    void 없으면_RESOURCE_NOT_FOUND() {
        given(memoryRepository.findByIdWithImages(memoryId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(guardianId, memoryId, "제목", "메모", "한마디", 2020, List.of()))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void 생성자가_아니면_NOT_RESOURCE_OWNER() {
        Memory memory = Memory.create(elderId, "제목", "메모", "한마디", 2020);
        // createdBy is null (unset by JPA auditing in unit test context) — guardianId.equals(null) is false
        given(memoryRepository.findByIdWithImages(memoryId)).willReturn(Optional.of(memory));

        assertThatThrownBy(() -> useCase.execute(guardianId, memoryId, "제목", "메모", "한마디", 2020, List.of()))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.NOT_RESOURCE_OWNER));
    }

    @Test
    void 미디어목록이_null이어도_이미지를_비우며_수정한다() throws Exception {
        Memory memory = Memory.create(elderId, "제목", "메모", "한마디", 2020);
        setCreatedBy(memory, guardianId);
        given(memoryRepository.findByIdWithImages(memoryId)).willReturn(Optional.of(memory));
        given(mediaUploadCommand.memoryImageMaxCount()).willReturn(4);

        useCase.execute(guardianId, memoryId, "새제목", "새메모", "새한마디", 2021,
                null, null, null);

        assertThat(memory.getTitle()).isEqualTo("새제목");
        assertThat(memory.getImages()).isEmpty();
    }
}
