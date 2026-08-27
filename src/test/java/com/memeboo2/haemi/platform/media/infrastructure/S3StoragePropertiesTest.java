package com.memeboo2.haemi.platform.media.infrastructure;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class S3StoragePropertiesTest {

    @Test
    void 기본값으로_생성한다() {
        S3StorageProperties props = new S3StorageProperties(
                "test-bucket", "ap-northeast-2", null, null, null,
                false, Duration.ofMinutes(15), Duration.ofHours(1));

        assertThat(props.bucket()).isEqualTo("test-bucket");
        assertThat(props.region()).isEqualTo("ap-northeast-2");
        assertThat(props.endpoint()).isNull();
        assertThat(props.accessKey()).isNull();
        assertThat(props.secretKey()).isNull();
        assertThat(props.pathStyleAccess()).isFalse();
        assertThat(props.presignedPutExpiry()).isEqualTo(Duration.ofMinutes(15));
        assertThat(props.servingUrlExpiry()).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void 커스텀_엔드포인트로_R2_호환_설정을_생성한다() {
        S3StorageProperties props = new S3StorageProperties(
                "r2-bucket", "auto", "https://r2.example.com",
                "access123", "secret456", true,
                Duration.ofMinutes(30), Duration.ofHours(2));

        assertThat(props.endpoint()).isEqualTo("https://r2.example.com");
        assertThat(props.accessKey()).isEqualTo("access123");
        assertThat(props.pathStyleAccess()).isTrue();
    }
}
