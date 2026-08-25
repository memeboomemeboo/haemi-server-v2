package com.memeboo2.haemi.guardian.memory;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.memory.application.RegisterMemoryUseCase;
import com.memeboo2.haemi.guardian.memory.domain.Memory;
import com.memeboo2.haemi.guardian.memory.domain.MemoryRegistered;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import com.memeboo2.haemi.platform.api.MediaPurpose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RegisterMemoryUseCaseTest {

    @Mock CareAccessQuery careAccessQuery;
    @Mock MemoryRepository memoryRepository;
    @Mock MediaUploadCommand mediaUploadCommand;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks RegisterMemoryUseCase useCase;

    UUID guardianId = UUID.randomUUID();
    UUID elderId    = UUID.randomUUID();

    @Test
    void 정상_등록_이벤트_발행() {
        UUID refId = UUID.randomUUID();
        given(mediaUploadCommand.memoryImageMaxCount()).willReturn(4);
        given(mediaUploadCommand.confirmUpload(guardianId, refId, MediaPurpose.MEMORY_IMAGE))
                .willReturn(URI.create("http://localhost/serve?key=memory_image/key.jpg"));
        given(memoryRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        useCase.execute(guardianId, elderId, "제목", "메모", "한마디", 2020, List.of(refId));

        ArgumentCaptor<MemoryRegistered> eventCaptor = ArgumentCaptor.forClass(MemoryRegistered.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().elderId()).isEqualTo(elderId);
        assertThat(eventCaptor.getValue().guardianId()).isEqualTo(guardianId);
    }

    @Test
    void 인가_실패는_403() {
        willThrow(new DomainException(ErrorCode.CARE_ACCESS_DENIED))
                .given(careAccessQuery).requireGuardianOf(guardianId, elderId);

        assertThatThrownBy(() -> useCase.execute(guardianId, elderId, "제목", null, "한마디", null, List.of()))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CARE_ACCESS_DENIED));
    }

    @Test
    void 이미지_5장_초과는_400() {
        List<UUID> fiveRefs = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID());
        given(mediaUploadCommand.memoryImageMaxCount()).willReturn(4);
        fiveRefs.forEach(refId ->
                lenient().when(mediaUploadCommand.confirmUpload(guardianId, refId, MediaPurpose.MEMORY_IMAGE))
                        .thenReturn(URI.create("http://localhost/serve?key=" + refId)));
        lenient().when(memoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> useCase.execute(guardianId, elderId, "제목", null, "한마디", null, fiveRefs))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void 메모_300자_초과는_400() {
        String longMemo = "a".repeat(301);
        assertThatThrownBy(() -> useCase.execute(guardianId, elderId, "제목", longMemo, "한마디", null, List.of()))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }
}
