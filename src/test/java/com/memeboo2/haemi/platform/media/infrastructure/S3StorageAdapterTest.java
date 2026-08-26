package com.memeboo2.haemi.platform.media.infrastructure;

import com.memeboo2.haemi.platform.media.domain.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3StorageAdapterTest {

    @Mock
    S3Client s3Client;
    @Mock
    S3Presigner presigner;

    private S3StorageAdapter adapter() {
        S3StorageProperties props = new S3StorageProperties(
                "haemi-bucket", "ap-northeast-2", null, "ak", "sk", false,
                Duration.ofMinutes(15), Duration.ofHours(1));
        return new S3StorageAdapter(s3Client, presigner, props);
    }

    @Test
    void buildStorageKey_는_타입과_확장자를_포함한다() {
        String key = adapter().buildStorageKey(MediaType.MEMORY_IMAGE, "photo.HEIC");
        assertThat(key).startsWith("memory_image/").endsWith(".HEIC");
    }

    @Test
    void headObject_는_컨텐츠타입_크기_및_사용자메타_길이를_읽는다() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(
                HeadObjectResponse.builder()
                        .contentType("audio/mp4")
                        .contentLength(1234L)
                        .metadata(Map.of("duration-seconds", "42"))
                        .build());

        Optional<StoragePort.ObjectMetadata> meta = adapter().headObject("voice/x.m4a");

        assertThat(meta).isPresent();
        assertThat(meta.get().contentType()).isEqualTo("audio/mp4");
        assertThat(meta.get().sizeBytes()).isEqualTo(1234L);
        assertThat(meta.get().durationSeconds()).isEqualTo(42);
    }

    @Test
    void headObject_는_없는_키에_대해_empty를_반환한다() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());

        assertThat(adapter().headObject("missing")).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getObject_는_바이트를_읽는다() {
        ResponseBytes<GetObjectResponse> bytes = (ResponseBytes<GetObjectResponse>) org.mockito.Mockito.mock(ResponseBytes.class);
        when(bytes.response()).thenReturn(GetObjectResponse.builder().contentType("image/jpeg").build());
        when(bytes.asByteArray()).thenReturn(new byte[]{1, 2, 3});
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(bytes);

        Optional<StoragePort.StoredContent> content = adapter().getObject("memory_image/x.jpg");

        assertThat(content).isPresent();
        assertThat(content.get().contentType()).isEqualTo("image/jpeg");
        assertThat(content.get().content()).containsExactly(1, 2, 3);
    }

    @Test
    void putObject_는_컨텐츠타입과_함께_저장한다() {
        adapter().putObject("memory_image/x.jpg", "image/jpeg", new byte[]{9});

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertThat(captor.getValue().key()).isEqualTo("memory_image/x.jpg");
        assertThat(captor.getValue().contentType()).isEqualTo("image/jpeg");
    }
}
