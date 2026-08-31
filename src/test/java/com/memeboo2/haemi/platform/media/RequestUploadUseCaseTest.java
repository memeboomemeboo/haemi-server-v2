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
import static org.mockito.Mockito.never;
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
            new UploadPolicyProperties.Voice(12_582_912L, 60, List.of("audio/aac", "audio/mp4")),
            new UploadPolicyProperties.Profile(5_242_880L, List.of("image/jpeg", "image/png", "image/webp")),
            new UploadPolicyProperties.PresignedUrl(Duration.ofMinutes(15)),
            new UploadPolicyProperties.Retention(365, 365)
    );

    @BeforeEach
    void setUp() {
        // lenient: 검증 실패 경로에서 호출되지 않는 stub을 허용
        lenient().when(clock.now()).thenReturn(NOW);
        lenient().when(storage.buildStorageKey(any(), any())).thenReturn("memory_image/test-key.jpg");
        lenient().when(storage.generatePresignedPutUrl(any(), any(), anyLong(), any()))
                .thenReturn(URI.create("http://localhost/presigned"));
        lenient().when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase = new RequestUploadUseCase(POLICY, storage, repository, clock);
    }

    @Test
    void 정상_이미지_발급() {
        UUID uploaderId = UUID.randomUUID();
        RequestUploadUseCase.Result result = useCase.request(
                uploaderId, MediaType.MEMORY_IMAGE, "photo.jpg", "image/jpeg", 1_000_000L, null);

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
                UUID.randomUUID(), MediaType.MEMORY_IMAGE, "file.gif", "image/gif", 500_000L, null))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void 파일_크기_초과는_400() {
        assertThatThrownBy(() -> useCase.request(
                UUID.randomUUID(), MediaType.MEMORY_IMAGE, "big.jpg", "image/jpeg", 10_485_761L, null))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void 원본_파일명이_255자를_넘으면_저장소를_호출하지_않고_400() {
        assertThatThrownBy(() -> useCase.request(
                UUID.randomUUID(), MediaType.MEMORY_IMAGE, "a".repeat(256) + ".jpg", "image/jpeg", 1_000_000L, null))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));

        verify(storage, never()).buildStorageKey(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void 음성_파일_발급() {
        RequestUploadUseCase.Result result = useCase.request(
                UUID.randomUUID(), MediaType.GREETING_VOICE, "hello.aac", "audio/aac", 1_000_000L, 60);

        assertThat(result.presignedUrl()).isNotNull();
        assertThat(result.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
    }

    @Test
    void 프로필_이미지_보관기간은_null() {
        ArgumentCaptor<MediaRef> captor = ArgumentCaptor.forClass(MediaRef.class);

        useCase.request(UUID.randomUUID(), MediaType.PROFILE_IMAGE, "profile.jpg", "image/jpeg", 1_000_000L, null);

        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getRetainUntil()).isNull();
    }

    @Test
    void 음성은_검증할_길이를_반드시_제공해야_한다() {
        assertThatThrownBy(() -> useCase.request(
                UUID.randomUUID(), MediaType.RESPONSE_VOICE, "answer.aac", "audio/aac", 1_000_000L, null))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void 일분을_초과한_음성은_업로드_URL을_발급하지_않는다() {
        assertThatThrownBy(() -> useCase.request(
                UUID.randomUUID(), MediaType.RESPONSE_VOICE, "answer.aac", "audio/aac", 1_000_000L, 61))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void 동일_업로더의_동일_해시는_중복으로_재사용된다() {
        UUID uploaderId = UUID.randomUUID();
        String hash = "A".repeat(64);
        MediaRef existing = MediaRef.pending(MediaType.MEMORY_IMAGE, "memory_image/existing.jpg", "old.jpg",
                "image/jpeg", 1_000_000L, null, uploaderId, NOW, null, hash.toLowerCase());
        given(repository.findFirstByUploaderIdAndMediaTypeAndContentHashAndStatus(
                uploaderId, MediaType.MEMORY_IMAGE, hash.toLowerCase(), UploadStatus.CONFIRMED))
                .willReturn(java.util.Optional.of(existing));
        given(storage.generateServingUrl("memory_image/existing.jpg"))
                .willReturn(URI.create("http://localhost/serve/existing"));

        RequestUploadUseCase.Result result = useCase.request(
                uploaderId, MediaType.MEMORY_IMAGE, "new.jpg", "image/jpeg", 1_000_000L, null, hash);

        assertThat(result.duplicate()).isTrue();
        assertThat(result.presignedUrl()).isNull();
        assertThat(result.servingUrl()).isEqualTo(URI.create("http://localhost/serve/existing"));
        verify(repository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void 해시가_있어도_기존이_없으면_정상_발급하고_해시를_저장한다() {
        UUID uploaderId = UUID.randomUUID();
        String hash = "b".repeat(64);
        given(repository.findFirstByUploaderIdAndMediaTypeAndContentHashAndStatus(
                uploaderId, MediaType.MEMORY_IMAGE, hash, UploadStatus.CONFIRMED))
                .willReturn(java.util.Optional.empty());

        RequestUploadUseCase.Result result = useCase.request(
                uploaderId, MediaType.MEMORY_IMAGE, "new.jpg", "image/jpeg", 1_000_000L, null, hash);

        assertThat(result.duplicate()).isFalse();
        assertThat(result.presignedUrl()).isNotNull();
        ArgumentCaptor<MediaRef> captor = ArgumentCaptor.forClass(MediaRef.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getContentHash()).isEqualTo(hash);
    }

    @Test
    void 동일_해시라도_다른_미디어_용도면_새_업로드를_발급한다() {
        UUID uploaderId = UUID.randomUUID();
        String hash = "c".repeat(64);
        given(repository.findFirstByUploaderIdAndMediaTypeAndContentHashAndStatus(
                uploaderId, MediaType.MEMORY_IMAGE, hash, UploadStatus.CONFIRMED))
                .willReturn(java.util.Optional.empty());

        RequestUploadUseCase.Result result = useCase.request(
                uploaderId, MediaType.MEMORY_IMAGE, "memory.jpg", "image/jpeg", 1_000_000L, null, hash);

        assertThat(result.duplicate()).isFalse();
        verify(repository).findFirstByUploaderIdAndMediaTypeAndContentHashAndStatus(
                uploaderId, MediaType.MEMORY_IMAGE, hash, UploadStatus.CONFIRMED);
        verify(repository).save(any(MediaRef.class));
    }

    @Test
    void 공백_해시는_무시하고_정상_발급한다() {
        UUID uploaderId = UUID.randomUUID();
        // contentHash.isBlank() true 분기 → normalizedHash=null, 중복조회 스킵
        RequestUploadUseCase.Result result = useCase.request(
                uploaderId, MediaType.MEMORY_IMAGE, "new.jpg", "image/jpeg", 1_000_000L, null, "   ");

        assertThat(result.duplicate()).isFalse();
        assertThat(result.presignedUrl()).isNotNull();
        ArgumentCaptor<MediaRef> captor = ArgumentCaptor.forClass(MediaRef.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getContentHash()).isNull();
    }

    @Test
    void 응답_이미지도_정상_발급된다() {
        RequestUploadUseCase.Result result = useCase.request(
                UUID.randomUUID(), MediaType.RESPONSE_IMAGE, "answer.png", "image/png", 500_000L, null);

        assertThat(result.presignedUrl()).isNotNull();
    }

    @Test
    void 이미지에_음성길이를_주면_400() {
        // mediaType이 음성이 아닌데 declaredDurationSeconds != null → INVALID_INPUT 분기
        assertThatThrownBy(() -> useCase.request(
                UUID.randomUUID(), MediaType.MEMORY_IMAGE, "photo.jpg", "image/jpeg", 1_000_000L, 10))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void 프로필_허용되지않는_타입은_400() {
        assertThatThrownBy(() -> useCase.request(
                UUID.randomUUID(), MediaType.PROFILE_IMAGE, "p.gif", "image/gif", 500_000L, null))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void 음성_허용되지않는_타입은_400() {
        assertThatThrownBy(() -> useCase.request(
                UUID.randomUUID(), MediaType.RESPONSE_VOICE, "a.mp3", "audio/mpeg", 500_000L, 30))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void 음성_크기_초과는_400() {
        assertThatThrownBy(() -> useCase.request(
                UUID.randomUUID(), MediaType.RESPONSE_VOICE, "a.aac", "audio/aac", 12_582_913L, 30))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void 음성_길이가_0이하면_400() {
        assertThatThrownBy(() -> useCase.request(
                UUID.randomUUID(), MediaType.RESPONSE_VOICE, "a.aac", "audio/aac", 500_000L, 0))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void 프로필_크기_초과는_400() {
        assertThatThrownBy(() -> useCase.request(
                UUID.randomUUID(), MediaType.PROFILE_IMAGE, "p.jpg", "image/jpeg", 5_242_881L, null))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }
}
