package com.memeboo2.haemi.platform.media.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** LocalObjectStorage의 메모리 기반 저장/조회/삭제 단위 테스트. */
class LocalObjectStorageTest {

    private LocalObjectStorage storage;

    @BeforeEach
    void setUp() {
        storage = new LocalObjectStorage();
    }

    @Test
    void 저장한_객체를_동일한_키로_조회할_수_있다() {
        byte[] content = {1, 2, 3};

        storage.put("voice/1.mp3", "audio/mpeg", content, 30);
        Optional<LocalObjectStorage.StoredObject> result = storage.get("voice/1.mp3");

        assertThat(result).isPresent();
        assertThat(result.get().contentType()).isEqualTo("audio/mpeg");
        assertThat(result.get().content()).containsExactly(1, 2, 3);
        assertThat(result.get().durationSeconds()).isEqualTo(30);
    }

    @Test
    void 존재하지_않는_키_조회시_빈_Optional을_반환한다() {
        Optional<LocalObjectStorage.StoredObject> result = storage.get("missing/1.mp3");

        assertThat(result).isEmpty();
    }

    @Test
    void 삭제한_객체는_더이상_조회되지_않는다() {
        storage.put("photo/1.jpg", "image/jpeg", new byte[] {1}, null);

        storage.remove("photo/1.jpg");

        assertThat(storage.get("photo/1.jpg")).isEmpty();
    }

    @Test
    void 같은_키로_저장하면_덮어쓴다() {
        storage.put("photo/1.jpg", "image/jpeg", new byte[] {1}, null);
        storage.put("photo/1.jpg", "image/png", new byte[] {2, 2}, null);

        Optional<LocalObjectStorage.StoredObject> result = storage.get("photo/1.jpg");

        assertThat(result).isPresent();
        assertThat(result.get().contentType()).isEqualTo("image/png");
        assertThat(result.get().content()).containsExactly(2, 2);
    }

    @Test
    void 저장된_바이트_배열은_외부에서_변경해도_내부_상태에_영향을_주지_않는다() {
        byte[] content = {1, 2, 3};
        storage.put("voice/1.mp3", "audio/mpeg", content, null);

        content[0] = 99;

        assertThat(storage.get("voice/1.mp3").get().content()).containsExactly(1, 2, 3);
    }

    @Test
    void duration이_null이어도_저장_조회_가능하다() {
        storage.put("photo/1.jpg", "image/jpeg", new byte[] {1}, null);

        Optional<LocalObjectStorage.StoredObject> result = storage.get("photo/1.jpg");

        assertThat(result).isPresent();
        assertThat(result.get().durationSeconds()).isNull();
    }
}
