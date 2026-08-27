package com.memeboo2.haemi.platform.media;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.platform.media.domain.MediaRef;
import com.memeboo2.haemi.platform.media.domain.MediaType;
import com.memeboo2.haemi.platform.media.domain.UploadStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaRefDomainTest {

    private final UUID uploaderId = UUID.randomUUID();

    private MediaRef pendingRef(Instant expiresAt) {
        return MediaRef.pending(
                MediaType.RESPONSE_IMAGE,
                "storage/key.jpg",
                "original.jpg",
                "image/jpeg",
                1024L,
                null,
                uploaderId,
                expiresAt,
                null,
                null);
    }

    @Test
    void pending은_PENDING_상태로_생성된다() {
        Instant expiresAt = Instant.now().plus(10, ChronoUnit.MINUTES);

        MediaRef ref = pendingRef(expiresAt);

        assertThat(ref.getStatus()).isEqualTo(UploadStatus.PENDING);
        assertThat(ref.getUploaderId()).isEqualTo(uploaderId);
        assertThat(ref.getStorageKey()).isEqualTo("storage/key.jpg");
    }

    @Test
    void confirm은_만료전이면_CONFIRMED로_전이한다() {
        MediaRef ref = pendingRef(Instant.now().plus(10, ChronoUnit.MINUTES));

        ref.confirm(Instant.now());

        assertThat(ref.getStatus()).isEqualTo(UploadStatus.CONFIRMED);
    }

    @Test
    void confirm은_이미_CONFIRMED면_멱등하게_동작한다() {
        MediaRef ref = pendingRef(Instant.now().plus(10, ChronoUnit.MINUTES));
        ref.confirm(Instant.now());

        ref.confirm(Instant.now());

        assertThat(ref.getStatus()).isEqualTo(UploadStatus.CONFIRMED);
    }

    @Test
    void confirm은_이미_EXPIRED면_예외를_던진다() {
        MediaRef ref = pendingRef(Instant.now().minus(1, ChronoUnit.MINUTES));
        assertThatThrownBy(() -> ref.confirm(Instant.now()))
                .isInstanceOf(DomainException.class);

        assertThatThrownBy(() -> ref.confirm(Instant.now()))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void confirm은_만료시각을_지나면_EXPIRED로_전이하며_예외를_던진다() {
        Instant expiresAt = Instant.now().minus(1, ChronoUnit.MINUTES);
        MediaRef ref = pendingRef(expiresAt);

        assertThatThrownBy(() -> ref.confirm(Instant.now()))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);

        assertThat(ref.getStatus()).isEqualTo(UploadStatus.EXPIRED);
    }

    @Test
    void ensureConfirmable은_CONFIRMED면_예외없이_통과한다() {
        MediaRef ref = pendingRef(Instant.now().plus(10, ChronoUnit.MINUTES));
        ref.confirm(Instant.now());

        ref.ensureConfirmable(Instant.now());

        assertThat(ref.getStatus()).isEqualTo(UploadStatus.CONFIRMED);
    }

    @Test
    void ensureConfirmable은_EXPIRED면_예외를_던진다() {
        MediaRef ref = pendingRef(Instant.now().minus(1, ChronoUnit.MINUTES));
        assertThatThrownBy(() -> ref.ensureConfirmable(Instant.now()))
                .isInstanceOf(DomainException.class);
        assertThat(ref.getStatus()).isEqualTo(UploadStatus.EXPIRED);

        assertThatThrownBy(() -> ref.ensureConfirmable(Instant.now()))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void ensureConfirmable은_만료시각을_지나면_EXPIRED로_전이하며_예외를_던진다() {
        MediaRef ref = pendingRef(Instant.now().minus(1, ChronoUnit.MINUTES));

        assertThatThrownBy(() -> ref.ensureConfirmable(Instant.now()))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);

        assertThat(ref.getStatus()).isEqualTo(UploadStatus.EXPIRED);
    }

    @Test
    void isOwnedBy는_업로더가_일치하면_true를_반환한다() {
        MediaRef ref = pendingRef(Instant.now().plus(10, ChronoUnit.MINUTES));

        assertThat(ref.isOwnedBy(uploaderId)).isTrue();
        assertThat(ref.isOwnedBy(UUID.randomUUID())).isFalse();
    }

    @Test
    void replaceStorage는_저장_정보를_갱신한다() {
        MediaRef ref = pendingRef(Instant.now().plus(10, ChronoUnit.MINUTES));

        ref.replaceStorage("storage/converted.jpg", "image/jpeg", 2048L);

        assertThat(ref.getStorageKey()).isEqualTo("storage/converted.jpg");
        assertThat(ref.getContentType()).isEqualTo("image/jpeg");
        assertThat(ref.getDeclaredSizeBytes()).isEqualTo(2048L);
    }
}
