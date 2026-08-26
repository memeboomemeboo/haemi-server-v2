package com.memeboo2.haemi.platform.media;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.platform.media.application.ConfirmUploadUseCase;
import com.memeboo2.haemi.platform.media.application.UploadPolicyProperties;
import com.memeboo2.haemi.platform.media.domain.MediaRef;
import com.memeboo2.haemi.platform.media.domain.MediaType;
import com.memeboo2.haemi.platform.media.domain.UploadStatus;
import com.memeboo2.haemi.platform.media.infrastructure.MediaRefRepository;
import com.memeboo2.haemi.platform.media.infrastructure.StoragePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ConfirmUploadUseCaseTest {

    @Mock MediaRefRepository repository;
    @Mock StoragePort storage;
    @Mock HaemiClock clock;
    @Mock UploadPolicyProperties policy;
    @Mock com.memeboo2.haemi.platform.media.application.HeicImageConverter heicConverter;
    @InjectMocks ConfirmUploadUseCase useCase;

    static final Instant NOW    = Instant.parse("2026-08-23T00:00:00Z");
    static final Instant EXPIRY = NOW.plusSeconds(900);

    private MediaRef pendingRef(UUID uploaderId) {
        return MediaRef.pending(
                MediaType.MEMORY_IMAGE, "memory_image/key.jpg", "photo.jpg",
                "image/jpeg", 1_000_000L, null, uploaderId, EXPIRY, NOW.plusSeconds(86400L * 365), null);
    }

    @Test
    void 정상_확정() {
        UUID actorId = UUID.randomUUID();
        UUID refId   = UUID.randomUUID();
        MediaRef ref = pendingRef(actorId);

        given(repository.findById(refId)).willReturn(Optional.of(ref));
        given(clock.now()).willReturn(NOW);
        given(storage.headObject(ref.getStorageKey())).willReturn(Optional.of(
                new StoragePort.ObjectMetadata("image/jpeg", 1_000_000L)));
        given(storage.generateServingUrl(any())).willReturn(URI.create("http://localhost/serve"));

        URI url = useCase.confirmUpload(actorId, refId);

        assertThat(url).isNotNull();
        assertThat(ref.getStatus()).isEqualTo(UploadStatus.CONFIRMED);
    }

    @Test
    void 타인_미디어_확정은_403() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID refId = UUID.randomUUID();

        given(repository.findById(refId)).willReturn(Optional.of(pendingRef(owner)));

        assertThatThrownBy(() -> useCase.confirmUpload(other, refId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.NOT_RESOURCE_OWNER));
    }

    @Test
    void 존재하지않는_미디어는_404() {
        UUID refId = UUID.randomUUID();
        given(repository.findById(refId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.confirmUpload(UUID.randomUUID(), refId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void presigned_URL_만료_후_확정은_EXPIRED() {
        UUID actorId = UUID.randomUUID();
        UUID refId   = UUID.randomUUID();
        MediaRef ref = pendingRef(actorId);

        given(repository.findById(refId)).willReturn(Optional.of(ref));
        given(clock.now()).willReturn(EXPIRY.plusSeconds(1));
        given(storage.headObject(ref.getStorageKey())).willReturn(Optional.of(
                new StoragePort.ObjectMetadata("image/jpeg", 1_000_000L)));

        assertThatThrownBy(() -> useCase.confirmUpload(actorId, refId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));

        assertThat(ref.getStatus()).isEqualTo(UploadStatus.EXPIRED);
    }

    @Test
    void 실제_업로드_객체가_없으면_확정할_수_없다() {
        UUID actorId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();
        MediaRef ref = pendingRef(actorId);
        given(repository.findById(refId)).willReturn(Optional.of(ref));
        given(storage.headObject(ref.getStorageKey())).willReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.confirmUpload(actorId, refId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
        assertThat(ref.getStatus()).isEqualTo(UploadStatus.PENDING);
    }

    @Test
    void 스토리지에서_검증한_음성이_1분을_넘으면_확정할_수_없다() {
        UUID actorId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();
        MediaRef ref = MediaRef.pending(MediaType.RESPONSE_VOICE, "response_voice/key.aac", "reply.aac",
                "audio/aac", 1_000_000L, 61, actorId, EXPIRY, NOW.plusSeconds(86400L), null);
        given(repository.findById(refId)).willReturn(Optional.of(ref));
        given(storage.headObject(ref.getStorageKey())).willReturn(Optional.of(
                new StoragePort.ObjectMetadata("audio/aac", 1_000_000L, 61)));
        given(policy.voice()).willReturn(new UploadPolicyProperties.Voice(
                12_582_912L, 60, List.of("audio/aac")));

        assertThatThrownBy(() -> useCase.confirmUpload(actorId, refId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void HEIC_이미지는_확정시_JPEG로_변환_재저장된다() {
        UUID actorId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();
        MediaRef ref = MediaRef.pending(MediaType.MEMORY_IMAGE, "memory_image/key.heic", "photo.heic",
                "image/heic", 2_000L, null, actorId, EXPIRY, NOW.plusSeconds(86400L * 365), null);

        given(repository.findById(refId)).willReturn(Optional.of(ref));
        given(clock.now()).willReturn(NOW);
        given(storage.headObject("memory_image/key.heic")).willReturn(Optional.of(
                new StoragePort.ObjectMetadata("image/heic", 2_000L)));
        given(storage.getObject("memory_image/key.heic")).willReturn(Optional.of(
                new StoragePort.StoredContent("image/heic", new byte[]{1, 2, 3})));
        given(heicConverter.toJpeg(any())).willReturn(new byte[]{10, 20, 30, 40});
        given(storage.generateServingUrl("memory_image/key.jpg"))
                .willReturn(URI.create("http://localhost/serve/key.jpg"));

        URI url = useCase.confirmUpload(actorId, refId);

        assertThat(url).isEqualTo(URI.create("http://localhost/serve/key.jpg"));
        assertThat(ref.getContentType()).isEqualTo("image/jpeg");
        assertThat(ref.getStorageKey()).isEqualTo("memory_image/key.jpg");
        assertThat(ref.getDeclaredSizeBytes()).isEqualTo(4L);
        org.mockito.Mockito.verify(storage).putObject("memory_image/key.jpg", "image/jpeg", new byte[]{10, 20, 30, 40});
        // 변환 후 원본 HEIC 객체는 정리된다.
        org.mockito.Mockito.verify(storage).deleteObject("memory_image/key.heic");
        assertThat(ref.getStatus()).isEqualTo(UploadStatus.CONFIRMED);
    }

    @Test
    void HEIC_변환_실패시_확정되지_않고_PENDING으로_남는다() {
        UUID actorId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();
        MediaRef ref = MediaRef.pending(MediaType.MEMORY_IMAGE, "memory_image/key.heic", "photo.heic",
                "image/heic", 2_000L, null, actorId, EXPIRY, NOW.plusSeconds(86400L * 365), null);

        given(repository.findById(refId)).willReturn(Optional.of(ref));
        given(clock.now()).willReturn(NOW);
        given(storage.headObject("memory_image/key.heic")).willReturn(Optional.of(
                new StoragePort.ObjectMetadata("image/heic", 2_000L)));
        given(storage.getObject("memory_image/key.heic")).willReturn(Optional.of(
                new StoragePort.StoredContent("image/heic", new byte[]{1, 2, 3})));
        given(heicConverter.toJpeg(any()))
                .willThrow(new DomainException(ErrorCode.MEDIA_CONVERSION_FAILED));

        assertThatThrownBy(() -> useCase.confirmUpload(actorId, refId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.MEDIA_CONVERSION_FAILED));

        // 변환 실패 → 상태 전이 없음, 스토리지 키 원본 유지, 정리도 없음.
        assertThat(ref.getStatus()).isEqualTo(UploadStatus.PENDING);
        assertThat(ref.getStorageKey()).isEqualTo("memory_image/key.heic");
        org.mockito.Mockito.verify(storage, org.mockito.Mockito.never()).deleteObject(any());
    }
}
