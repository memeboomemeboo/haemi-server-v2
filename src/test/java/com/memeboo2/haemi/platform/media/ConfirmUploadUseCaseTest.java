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
import com.memeboo2.haemi.platform.api.MediaPurpose;
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
    void 확정된_음성만_STT_후속처리를_위해_원본으로_읽을_수_있다() {
        UUID actorId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();
        MediaRef ref = MediaRef.pending(MediaType.RESPONSE_VOICE, "response_voice/key.aac", "reply.aac",
                "audio/aac", 3L, 12, actorId, EXPIRY, NOW.plusSeconds(86400L), null);
        ref.confirm(NOW);
        given(repository.findById(refId)).willReturn(Optional.of(ref));
        given(storage.getObject(ref.getStorageKey())).willReturn(Optional.of(
                new StoragePort.StoredContent("audio/aac", new byte[]{1, 2, 3})));

        var media = useCase.readConfirmedMedia(refId, MediaPurpose.RESPONSE_VOICE);

        assertThat(media).isPresent();
        assertThat(media.orElseThrow().contentType()).isEqualTo("audio/aac");
        assertThat(media.orElseThrow().content()).containsExactly(1, 2, 3);
    }

    @Test
    void 아직_확정되지_않은_음성은_STT_후속처리에서_읽을_수_없다() {
        UUID actorId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();
        MediaRef ref = MediaRef.pending(MediaType.RESPONSE_VOICE, "response_voice/key.aac", "reply.aac",
                "audio/aac", 3L, 12, actorId, EXPIRY, NOW.plusSeconds(86400L), null);
        given(repository.findById(refId)).willReturn(Optional.of(ref));

        var media = useCase.readConfirmedMedia(refId, MediaPurpose.RESPONSE_VOICE);

        assertThat(media).isEmpty();
        org.mockito.Mockito.verifyNoInteractions(storage);
    }

    @Test
    void 응답_음성은_확정시_임시_업로드_키와_분리된_서버_전용_키를_사용한다() {
        UUID actorId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();
        String temporaryKey = "response_voice/temporary.aac";
        String confirmedKey = "response_voice/confirmed.aac";
        MediaRef ref = MediaRef.pending(MediaType.RESPONSE_VOICE, temporaryKey, "reply.aac",
                "audio/aac", 3L, 12, actorId, EXPIRY, NOW.plusSeconds(86400L), null);
        given(repository.findById(refId)).willReturn(Optional.of(ref));
        given(storage.headObject(temporaryKey)).willReturn(Optional.of(
                new StoragePort.ObjectMetadata("audio/aac", 3L, 12, "etag-before-copy")));
        given(policy.voice()).willReturn(new UploadPolicyProperties.Voice(
                12_582_912L, 60, List.of("audio/aac")));
        given(clock.now()).willReturn(NOW);
        given(storage.buildStorageKey(MediaType.RESPONSE_VOICE, "reply.aac")).willReturn(confirmedKey);
        given(storage.generateServingUrl(confirmedKey)).willReturn(URI.create("http://localhost/serve/confirmed"));

        URI servingUrl = useCase.confirmUpload(actorId, refId, MediaPurpose.RESPONSE_VOICE);

        assertThat(servingUrl).isEqualTo(URI.create("http://localhost/serve/confirmed"));
        assertThat(ref.getStorageKey()).isEqualTo(confirmedKey);
        assertThat(ref.getStatus()).isEqualTo(UploadStatus.CONFIRMED);
        org.mockito.Mockito.verify(storage).copyObject(temporaryKey, confirmedKey, "etag-before-copy");
        org.mockito.Mockito.verify(storage).deleteObject(temporaryKey);
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
    void expectedPurpose_일치하면_정상_확정된다() {
        UUID actorId = UUID.randomUUID();
        UUID refId   = UUID.randomUUID();
        MediaRef ref = pendingRef(actorId); // MEMORY_IMAGE

        given(repository.findById(refId)).willReturn(Optional.of(ref));
        given(clock.now()).willReturn(NOW);
        given(storage.headObject(ref.getStorageKey())).willReturn(Optional.of(
                new StoragePort.ObjectMetadata("image/jpeg", 1_000_000L)));
        given(storage.generateServingUrl(any())).willReturn(URI.create("http://localhost/serve"));

        URI url = useCase.confirmUpload(actorId, refId,
                com.memeboo2.haemi.platform.api.MediaPurpose.MEMORY_IMAGE);

        assertThat(url).isNotNull();
        assertThat(ref.getStatus()).isEqualTo(UploadStatus.CONFIRMED);
    }

    @Test
    void expectedPurpose_불일치하면_INVALID_INPUT() {
        UUID actorId = UUID.randomUUID();
        UUID refId   = UUID.randomUUID();
        MediaRef ref = pendingRef(actorId); // MEMORY_IMAGE

        given(repository.findById(refId)).willReturn(Optional.of(ref));

        assertThatThrownBy(() -> useCase.confirmUpload(actorId, refId,
                com.memeboo2.haemi.platform.api.MediaPurpose.PROFILE_IMAGE))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void 스토리지_contentType이_요청과_다르면_INVALID_INPUT() {
        UUID actorId = UUID.randomUUID();
        UUID refId   = UUID.randomUUID();
        MediaRef ref = pendingRef(actorId);

        given(repository.findById(refId)).willReturn(Optional.of(ref));
        given(storage.headObject(ref.getStorageKey())).willReturn(Optional.of(
                new StoragePort.ObjectMetadata("image/png", 1_000_000L)));

        assertThatThrownBy(() -> useCase.confirmUpload(actorId, refId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void 스토리지_사이즈가_요청과_다르면_INVALID_INPUT() {
        UUID actorId = UUID.randomUUID();
        UUID refId   = UUID.randomUUID();
        MediaRef ref = pendingRef(actorId);

        given(repository.findById(refId)).willReturn(Optional.of(ref));
        given(storage.headObject(ref.getStorageKey())).willReturn(Optional.of(
                new StoragePort.ObjectMetadata("image/jpeg", 999L)));

        assertThatThrownBy(() -> useCase.confirmUpload(actorId, refId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void 음성_길이_메타데이터가_없으면_INVALID_INPUT() {
        UUID actorId = UUID.randomUUID();
        UUID refId   = UUID.randomUUID();
        MediaRef ref = MediaRef.pending(MediaType.RESPONSE_VOICE, "response_voice/key.aac", "reply.aac",
                "audio/aac", 1_000_000L, 30, actorId, EXPIRY, NOW.plusSeconds(86400L), null);
        given(repository.findById(refId)).willReturn(Optional.of(ref));
        given(storage.headObject(ref.getStorageKey())).willReturn(Optional.of(
                new StoragePort.ObjectMetadata("audio/aac", 1_000_000L, null)));

        assertThatThrownBy(() -> useCase.confirmUpload(actorId, refId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void 기대하는_음성길이와_실제가_다르면_INVALID_INPUT() {
        UUID actorId = UUID.randomUUID();
        UUID refId   = UUID.randomUUID();
        MediaRef ref = MediaRef.pending(MediaType.RESPONSE_VOICE, "response_voice/key.aac", "reply.aac",
                "audio/aac", 1_000_000L, 30, actorId, EXPIRY, NOW.plusSeconds(86400L), null);
        given(repository.findById(refId)).willReturn(Optional.of(ref));
        given(storage.headObject(ref.getStorageKey())).willReturn(Optional.of(
                new StoragePort.ObjectMetadata("audio/aac", 1_000_000L, 30)));
        given(policy.voice()).willReturn(new UploadPolicyProperties.Voice(
                12_582_912L, 60, List.of("audio/aac")));

        // expectedDurationSeconds=40 이지만 실제=30 → 불일치
        assertThatThrownBy(() -> useCase.confirmUpload(actorId, refId, null, 40))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void HEIC_원본정리_실패해도_확정은_성공한다() {
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
        org.mockito.Mockito.doThrow(new RuntimeException("s3 down"))
                .when(storage).deleteObject("memory_image/key.heic");

        URI url = useCase.confirmUpload(actorId, refId);

        // best-effort 정리 실패는 확정 결과를 막지 않는다.
        assertThat(url).isEqualTo(URI.create("http://localhost/serve/key.jpg"));
        assertThat(ref.getStatus()).isEqualTo(UploadStatus.CONFIRMED);
    }

    @Test
    void HEIC_확장자없는_키는_jpg가_덧붙는다() {
        UUID actorId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();
        // 확장자 dot이 slash보다 앞 → toJpegKey else 분기(원본키 + ".jpg")
        MediaRef ref = MediaRef.pending(MediaType.MEMORY_IMAGE, "memory_image/keynoext", "photo.heic",
                "image/heic", 2_000L, null, actorId, EXPIRY, NOW.plusSeconds(86400L * 365), null);

        given(repository.findById(refId)).willReturn(Optional.of(ref));
        given(clock.now()).willReturn(NOW);
        given(storage.headObject("memory_image/keynoext")).willReturn(Optional.of(
                new StoragePort.ObjectMetadata("image/heic", 2_000L)));
        given(storage.getObject("memory_image/keynoext")).willReturn(Optional.of(
                new StoragePort.StoredContent("image/heic", new byte[]{1, 2, 3})));
        given(heicConverter.toJpeg(any())).willReturn(new byte[]{10, 20, 30, 40});
        given(storage.generateServingUrl("memory_image/keynoext.jpg"))
                .willReturn(URI.create("http://localhost/serve/keynoext.jpg"));

        URI url = useCase.confirmUpload(actorId, refId);

        assertThat(url).isEqualTo(URI.create("http://localhost/serve/keynoext.jpg"));
        assertThat(ref.getStorageKey()).isEqualTo("memory_image/keynoext.jpg");
    }

    @Test
    void 정상_음성_확정_GREETING_VOICE() {
        UUID actorId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();
        // isVoice의 GREETING_VOICE 분기 + 음성 검증 통과 경로(모든 조건 false)
        MediaRef ref = MediaRef.pending(MediaType.GREETING_VOICE, "greeting_voice/key.aac", "hi.aac",
                "audio/aac", 1_000_000L, 30, actorId, EXPIRY, NOW.plusSeconds(86400L), null);
        given(repository.findById(refId)).willReturn(Optional.of(ref));
        given(clock.now()).willReturn(NOW);
        given(storage.headObject(ref.getStorageKey())).willReturn(Optional.of(
                new StoragePort.ObjectMetadata("audio/aac", 1_000_000L, 30)));
        given(policy.voice()).willReturn(new UploadPolicyProperties.Voice(
                12_582_912L, 60, List.of("audio/aac")));
        given(storage.generateServingUrl(any())).willReturn(URI.create("http://localhost/serve"));

        URI url = useCase.confirmUpload(actorId, refId, null, 30); // expectedDuration 일치

        assertThat(url).isNotNull();
        assertThat(ref.getStatus()).isEqualTo(UploadStatus.CONFIRMED);
    }

    @Test
    void 선언한_음성길이와_실제_저장길이가_다르면_INVALID_INPUT() {
        UUID actorId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();
        MediaRef ref = MediaRef.pending(MediaType.RESPONSE_VOICE, "response_voice/key.aac", "r.aac",
                "audio/aac", 1_000_000L, 30, actorId, EXPIRY, NOW.plusSeconds(86400L), null);
        given(repository.findById(refId)).willReturn(Optional.of(ref));
        given(storage.headObject(ref.getStorageKey())).willReturn(Optional.of(
                new StoragePort.ObjectMetadata("audio/aac", 1_000_000L, 25))); // 선언 30 ≠ 실제 25

        assertThatThrownBy(() -> useCase.confirmUpload(actorId, refId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void RESPONSE_IMAGE_HEIC도_JPEG로_변환된다() {
        UUID actorId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();
        // isHeicImage의 RESPONSE_IMAGE 분기
        MediaRef ref = MediaRef.pending(MediaType.RESPONSE_IMAGE, "response_image/key.heic", "r.heic",
                "image/heif", 2_000L, null, actorId, EXPIRY, NOW.plusSeconds(86400L * 365), null);
        given(repository.findById(refId)).willReturn(Optional.of(ref));
        given(clock.now()).willReturn(NOW);
        given(storage.headObject("response_image/key.heic")).willReturn(Optional.of(
                new StoragePort.ObjectMetadata("image/heif", 2_000L)));
        given(storage.getObject("response_image/key.heic")).willReturn(Optional.of(
                new StoragePort.StoredContent("image/heif", new byte[]{1, 2, 3})));
        given(heicConverter.toJpeg(any())).willReturn(new byte[]{9, 9});
        given(storage.generateServingUrl("response_image/key.jpg"))
                .willReturn(URI.create("http://localhost/serve/r.jpg"));

        URI url = useCase.confirmUpload(actorId, refId);

        assertThat(url).isNotNull();
        assertThat(ref.getContentType()).isEqualTo("image/jpeg");
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
