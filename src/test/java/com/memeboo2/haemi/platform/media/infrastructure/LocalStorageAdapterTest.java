package com.memeboo2.haemi.platform.media.infrastructure;

import com.memeboo2.haemi.platform.media.domain.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocalStorageAdapterTest {

    @Mock LocalObjectStorage objectStorage;
    @InjectMocks LocalStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        // no-op: 각 테스트에서 필요한 스텁만 개별 설정
    }

    @Test
    void presigned_put_url은_key와_contentType을_인코딩해_포함한다() {
        URI uri = adapter.generatePresignedPutUrl("image/foo bar.jpg", "image/jpeg", 60L, null);

        assertThat(uri.toString()).contains("key=image%2Ffoo+bar.jpg");
        assertThat(uri.toString()).contains("contentType=image%2Fjpeg");
        assertThat(uri.toString()).doesNotContain("durationSeconds");
    }

    @Test
    void presigned_put_url은_durationSeconds가_있으면_쿼리에_포함한다() {
        URI uri = adapter.generatePresignedPutUrl("voice/abc.m4a", "audio/mp4", 60L, 30);

        assertThat(uri.toString()).contains("durationSeconds=30");
    }

    @Test
    void serving_url은_key를_인코딩해_포함한다() {
        URI uri = adapter.generateServingUrl("image/foo bar.jpg");

        assertThat(uri.toString()).contains("key=image%2Ffoo+bar.jpg");
        assertThat(uri.toString()).contains("/storage/serve");
    }

    @Test
    void storage_key는_미디어타입_소문자와_확장자를_포함한다() {
        String key = adapter.buildStorageKey(MediaType.MEMORY_IMAGE, "photo.jpeg");

        assertThat(key).startsWith("memory_image/");
        assertThat(key).endsWith(".jpeg");
    }

    @Test
    void storage_key는_확장자가_없으면_빈_확장자를_사용한다() {
        String key = adapter.buildStorageKey(MediaType.MEMORY_IMAGE, "noext");

        assertThat(key).startsWith("memory_image/");
        assertThat(key).doesNotContain(".");
    }

    @Test
    void headObject는_존재하는_객체의_메타데이터를_반환한다() {
        given(objectStorage.get("k")).willReturn(Optional.of(
                new LocalObjectStorage.StoredObject("image/jpeg", new byte[]{1, 2, 3}, 5)));

        Optional<StoragePort.ObjectMetadata> meta = adapter.headObject("k");

        assertThat(meta).isPresent();
        assertThat(meta.get().contentType()).isEqualTo("image/jpeg");
        assertThat(meta.get().sizeBytes()).isEqualTo(3);
        assertThat(meta.get().durationSeconds()).isEqualTo(5);
    }

    @Test
    void headObject는_없는_객체면_빈값을_반환한다() {
        given(objectStorage.get("missing")).willReturn(Optional.empty());

        assertThat(adapter.headObject("missing")).isEmpty();
    }

    @Test
    void getObject는_존재하는_객체의_내용을_반환한다() {
        given(objectStorage.get("k")).willReturn(Optional.of(
                new LocalObjectStorage.StoredObject("image/jpeg", new byte[]{9, 9}, null)));

        Optional<StoragePort.StoredContent> content = adapter.getObject("k");

        assertThat(content).isPresent();
        assertThat(content.get().contentType()).isEqualTo("image/jpeg");
        assertThat(content.get().content()).containsExactly(9, 9);
    }

    @Test
    void copyObject는_임시_객체의_타입_내용_길이를_확정_키에_보존한다() {
        given(objectStorage.get("temporary")).willReturn(Optional.of(
                new LocalObjectStorage.StoredObject("audio/aac", new byte[]{9, 9}, 12)));

        adapter.copyObject("temporary", "confirmed", "etag");

        verify(objectStorage).put(eq("confirmed"), eq("audio/aac"), any(byte[].class), eq(12));
    }

    @Test
    void putObject는_기존_durationSeconds를_유지하며_저장한다() {
        given(objectStorage.get("k")).willReturn(Optional.of(
                new LocalObjectStorage.StoredObject("image/jpeg", new byte[]{1}, 42)));

        adapter.putObject("k", "image/png", new byte[]{2, 3});

        verify(objectStorage).put(eq("k"), eq("image/png"), any(byte[].class), eq(42));
    }

    @Test
    void putObject는_기존_객체가_없으면_durationSeconds_null로_저장한다() {
        given(objectStorage.get("k")).willReturn(Optional.empty());

        adapter.putObject("k", "image/png", new byte[]{2, 3});

        verify(objectStorage).put(eq("k"), eq("image/png"), any(byte[].class), eq(null));
    }

    @Test
    void deleteObject는_저장소에_위임한다() {
        adapter.deleteObject("k");

        verify(objectStorage).remove("k");
    }
}
