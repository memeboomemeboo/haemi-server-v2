package com.memeboo2.haemi.platform.media;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.platform.media.application.RequestUploadUseCase;
import com.memeboo2.haemi.platform.media.application.UploadPolicyProperties;
import com.memeboo2.haemi.platform.media.domain.MediaRef;
import com.memeboo2.haemi.platform.media.domain.MediaType;
import com.memeboo2.haemi.platform.media.domain.UploadStatus;
import com.memeboo2.haemi.platform.media.infrastructure.MediaRefRepository;
import com.memeboo2.haemi.platform.media.infrastructure.StoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RequestUploadUseCaseTest {

    @Mock MediaRefRepository repository;
    @Mock StoragePort storage;
    @Mock HaemiClock clock;

    RequestUploadUseCase useCase;

    static final Instant NOW = Instant.parse("2026-08-23T00:00:00Z");

    static final UploadPolicyProperties POLICY = new UploadPolicyProperties(
            new UploadPolicyProperties.Image(10_485_760L, 4, List.of("image/jpeg", "image/png", "image/webp")),
            new UploadPolicyProperties.Voice(12_582_912L, 180, List.of("audio/aac", "audio/mp4")),
            new UploadPolicyProperties.Profile(5_242_880L, List.of("image/jpeg", "image/png", "image/webp")),
            new UploadPolicyProperties.PresignedUrl(Duration.ofMinutes(15)),
            new UploadPolicyProperties.Retention(365, 365)
    );

    @BeforeEach
    void setUp() {
        // lenient: 검증 실패 경로에서 호출되지 않는 stub을 허용
        lenient().when(clock.now()).thenReturn(NOW);
        lenient().when(storage.buildStorageKey(any(), any())).thenReturn("memory_image/test-key.jpg");
        lenient().when(storage.generatePresignedPutUrl(any(), any(), anyLong()))
                .thenReturn(URI.create("http://localhost/presigned"));
        lenient().when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase = new RequestUploadUseCase(POLICY, storage, repository, clock);
    }

    @Test
    void 정상_이미지_발급() {
        UUID uploaderId = UUID.randomUUID();
        RequestUploadUseCase.Result result = useCase.request(
                uploaderId, MediaType.MEMORY_IMAGE, "photo.jpg", "image/jpeg", 1_000_000L);

        assertThat(result.presignedUrl()).isNotNull();
        assertThat(result.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));

        ArgumentCaptor<MediaRef> captor = ArgumentCaptor.forClass(MediaRef.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(UploadStatus.PENDING);
        assertThat(captor.getValue().getUploaderId()).isEqualTo(uploaderId);
    }

    @Test
    void 허용되지않는_content_type은_400() {
        assertThatThrownBy(() -> useCase.request(
                UUID.randomUUID(), MediaType.MEMORY_IMAGE, "file.gif", "image/gif", 500_000L))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void 파일_크기_초과는_400() {
        assertThatThrownBy(() -> useCase.request(
                UUID.randomUUID(), MediaType.MEMORY_IMAGE, "big.jpg", "image/jpeg", 10_485_761L))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void 음성_파일_발급() {
        RequestUploadUseCase.Result result = useCase.request(
                UUID.randomUUID(), MediaType.GREETING_VOICE, "hello.aac", "audio/aac", 1_000_000L);

        assertThat(result.presignedUrl()).isNotNull();
        assertThat(result.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
    }

    @Test
    void 프로필_이미지_보관기간은_null() {
        ArgumentCaptor<MediaRef> captor = ArgumentCaptor.forClass(MediaRef.class);

        useCase.request(UUID.randomUUID(), MediaType.PROFILE_IMAGE, "profile.jpg", "image/jpeg", 1_000_000L);

        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getRetainUntil()).isNull();
    }
}
