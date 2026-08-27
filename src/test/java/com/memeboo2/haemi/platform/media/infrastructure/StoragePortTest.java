package com.memeboo2.haemi.platform.media.infrastructure;

import com.memeboo2.haemi.platform.media.domain.MediaType;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * StoragePort의 default 메서드(미지원 어댑터에서의 기본 동작) 단위 테스트.
 * 최소 구현체를 통해 headObject/getObject/putObject/deleteObject의 기본값을 검증한다.
 */
class StoragePortTest {

    private final StoragePort minimalAdapter = new StoragePort() {
        @Override
        public URI generatePresignedPutUrl(String storageKey, String contentType, long expirySeconds,
                                           Integer expectedDurationSeconds) {
            return URI.create("https://example.com/" + storageKey);
        }

        @Override
        public URI generateServingUrl(String storageKey) {
            return URI.create("https://example.com/serve/" + storageKey);
        }

        @Override
        public String buildStorageKey(MediaType mediaType, String originalFilename) {
            return mediaType.name().toLowerCase() + "/" + originalFilename;
        }
    };

    @Test
    void headObject_기본구현은_빈_Optional을_반환한다() {
        Optional<StoragePort.ObjectMetadata> result = minimalAdapter.headObject("any-key");

        assertThat(result).isEmpty();
    }

    @Test
    void getObject_기본구현은_UnsupportedOperationException을_던진다() {
        assertThatThrownBy(() -> minimalAdapter.getObject("any-key"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void putObject_기본구현은_UnsupportedOperationException을_던진다() {
        assertThatThrownBy(() -> minimalAdapter.putObject("any-key", "text/plain", new byte[] {1}))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void deleteObject_기본구현은_예외없이_동작한다() {
        minimalAdapter.deleteObject("missing-key");
    }

    @Test
    void 필수_메서드는_구현체_동작을_그대로_사용한다() {
        assertThat(minimalAdapter.generatePresignedPutUrl("k", "image/jpeg", 60, null))
                .isEqualTo(URI.create("https://example.com/k"));
        assertThat(minimalAdapter.generateServingUrl("k"))
                .isEqualTo(URI.create("https://example.com/serve/k"));
        assertThat(minimalAdapter.buildStorageKey(MediaType.MEMORY_IMAGE, "a.jpg"))
                .isEqualTo("memory_image/a.jpg");
    }

    @Test
    void ObjectMetadata_2인자_생성자는_durationSeconds를_null로_설정한다() {
        StoragePort.ObjectMetadata metadata = new StoragePort.ObjectMetadata("image/jpeg", 1024L);

        assertThat(metadata.contentType()).isEqualTo("image/jpeg");
        assertThat(metadata.sizeBytes()).isEqualTo(1024L);
        assertThat(metadata.durationSeconds()).isNull();
    }

    @Test
    void ObjectMetadata_3인자_생성자는_durationSeconds를_보존한다() {
        StoragePort.ObjectMetadata metadata = new StoragePort.ObjectMetadata("audio/mpeg", 2048L, 45);

        assertThat(metadata.durationSeconds()).isEqualTo(45);
    }

    @Test
    void StoredContent는_contentType과_바이트를_보존한다() {
        StoragePort.StoredContent content = new StoragePort.StoredContent("image/png", new byte[] {1, 2});

        assertThat(content.contentType()).isEqualTo("image/png");
        assertThat(content.content()).containsExactly(1, 2);
    }
}
