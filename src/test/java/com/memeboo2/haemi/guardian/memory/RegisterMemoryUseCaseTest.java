package com.memeboo2.haemi.guardian.memory;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.memory.application.RegisterMemoryUseCase;
import com.memeboo2.haemi.guardian.memory.domain.Memory;
import com.memeboo2.haemi.guardian.memory.domain.MemoryRegistered;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
import com.memeboo2.haemi.platform.api.MediaPurpose;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class RegisterMemoryUseCaseTest {

    @Mock CareAccessQuery careAccessQuery;
    @Mock MemoryRepository memoryRepository;
    @Mock MediaUploadCommand mediaUploadCommand;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks RegisterMemoryUseCase useCase;

    UUID guardianId = UUID.randomUUID();
    UUID elderId = UUID.randomUUID();

    @Test
    void 정상_경로_등록_및_이벤트_발행() {
        UUID mediaRefId = UUID.randomUUID();
        given(mediaUploadCommand.memoryImageMaxCount()).willReturn(4);
        given(mediaUploadCommand.confirmUploadKey(guardianId, mediaRefId, MediaPurpose.MEMORY_IMAGE))
                .willReturn("memory_image/confirmed.png");
        given(memoryRepository.save(any(Memory.class))).willAnswer(invocation -> {
            Memory saved = invocation.getArgument(0);
            java.lang.reflect.Field idField =
                    com.memeboo2.haemi.common.persistence.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(saved, UUID.randomUUID());
            return saved;
        });

        UUID result = useCase.execute(guardianId, elderId, "제목", "메모", "한마디", 2020, 4, "구지면",
                List.of(mediaRefId));

        assertThat(result).isNotNull();
        ArgumentCaptor<MemoryRegistered> eventCaptor = ArgumentCaptor.forClass(MemoryRegistered.class);
        then(eventPublisher).should().publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().elderId()).isEqualTo(elderId);
        assertThat(eventCaptor.getValue().guardianId()).isEqualTo(guardianId);
        ArgumentCaptor<Memory> memoryCaptor = ArgumentCaptor.forClass(Memory.class);
        then(memoryRepository).should().save(memoryCaptor.capture());
        assertThat(memoryCaptor.getValue().getMemoryMonth()).isEqualTo(4);
        assertThat(memoryCaptor.getValue().getPlace()).isEqualTo("구지면");
        assertThat(memoryCaptor.getValue().getImages()).extracting(image -> image.getStorageKey())
                .containsExactly("memory_image/confirmed.png");
    }

    @Test
    void 링크없는_보호자는_CARE_ACCESS_DENIED() {
        willThrow(new DomainException(ErrorCode.CARE_ACCESS_DENIED))
                .given(careAccessQuery).requireGuardianOf(guardianId, elderId);

        assertThatThrownBy(() -> useCase.execute(guardianId, elderId, "제목", "메모", "한마디", 2020, List.of()))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CARE_ACCESS_DENIED));

        then(memoryRepository).should(org.mockito.Mockito.never()).save(any());
    }

    @Test
    void 미디어목록이_null이어도_이미지없이_등록한다() throws Exception {
        given(mediaUploadCommand.memoryImageMaxCount()).willReturn(4);
        given(memoryRepository.save(any(Memory.class))).willAnswer(invocation -> {
            Memory saved = invocation.getArgument(0);
            java.lang.reflect.Field idField =
                    com.memeboo2.haemi.common.persistence.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(saved, UUID.randomUUID());
            return saved;
        });

        UUID result = useCase.execute(guardianId, elderId, "제목", null, "한마디", 2020,
                null, null, null);

        assertThat(result).isNotNull();
        then(mediaUploadCommand).should(org.mockito.Mockito.never())
                .confirmUploadKey(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 이미지_너무_많으면_INVALID_INPUT() {
        given(mediaUploadCommand.memoryImageMaxCount()).willReturn(4);
        List<UUID> tooMany = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID());

        assertThatThrownBy(() -> useCase.execute(guardianId, elderId, "제목", "메모", "한마디", 2020, tooMany))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));

        then(memoryRepository).should(org.mockito.Mockito.never()).save(any());
    }
}
