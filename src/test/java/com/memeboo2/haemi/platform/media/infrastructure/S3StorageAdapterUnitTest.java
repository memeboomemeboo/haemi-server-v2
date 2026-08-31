package com.memeboo2.haemi.platform.media.infrastructure;

import com.memeboo2.haemi.platform.media.domain.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** S3StorageAdapter의 S3Client/S3Presigner 연동 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class S3StorageAdapterUnitTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner presigner;

    private S3StorageProperties props;
    private S3StorageAdapter adapter;

    @BeforeEach
    void setUp() {
        props = new S3StorageProperties(
                "test-bucket", "ap-northeast-2", null, null, null,
                false, Duration.ofMinutes(15), Duration.ofHours(1));
        adapter = new S3StorageAdapter(s3Client, presigner, props);
    }

    @Nested
    class 프리사인드_URL_발급 {

        @Test
        void PUT_프리사인드_URL을_발급한다() throws Exception {
            PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
            when(presigned.url()).thenReturn(new URL("https://test-bucket.s3.amazonaws.com/voice/1.mp3?sig=abc"));
            when(presigner.presignPutObject(any(software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest.class)))
                    .thenReturn(presigned);

            URI result = adapter.generatePresignedPutUrl("voice/1.mp3", "audio/mpeg", 900L, 30);

            assertThat(result).isEqualTo(URI.create("https://test-bucket.s3.amazonaws.com/voice/1.mp3?sig=abc"));
        }

        @Test
        void duration_메타데이터가_없어도_PUT_URL을_발급한다() throws Exception {
            PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
            when(presigned.url()).thenReturn(new URL("https://test-bucket.s3.amazonaws.com/photo/1.jpg"));
            when(presigner.presignPutObject(any(software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest.class)))
                    .thenReturn(presigned);

            URI result = adapter.generatePresignedPutUrl("photo/1.jpg", "image/jpeg", 900L, null);

            assertThat(result.toString()).contains("photo/1.jpg");
        }

        @Test
        void GET_서빙_URL을_발급한다() throws Exception {
            PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
            when(presigned.url()).thenReturn(new URL("https://test-bucket.s3.amazonaws.com/voice/1.mp3?sig=xyz"));
            when(presigner.presignGetObject(any(software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.class)))
                    .thenReturn(presigned);

            URI result = adapter.generateServingUrl("voice/1.mp3");

            assertThat(result).isEqualTo(URI.create("https://test-bucket.s3.amazonaws.com/voice/1.mp3?sig=xyz"));
        }
    }

    @Nested
    class 스토리지_키_생성 {

        @Test
        void 확장자를_보존해_스토리지_키를_생성한다() {
            String key = adapter.buildStorageKey(MediaType.RESPONSE_VOICE, "recording.mp3");

            assertThat(key).startsWith("response_voice/");
            assertThat(key).endsWith(".mp3");
        }

        @Test
        void 확장자가_없으면_빈_확장자로_생성한다() {
            String key = adapter.buildStorageKey(MediaType.MEMORY_IMAGE, "noext");

            assertThat(key).startsWith("memory_image/");
            assertThat(key).doesNotContain(".");
        }
    }

    @Nested
    class 메타데이터_조회 {

        @Test
        void 존재하는_객체의_메타데이터를_조회한다() {
            HeadObjectResponse response = HeadObjectResponse.builder()
                    .contentType("audio/mpeg")
                    .contentLength(1024L)
                    .metadata(Map.of("duration-seconds", "42"))
                    .eTag("etag-1")
                    .build();
            when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(response);

            Optional<StoragePort.ObjectMetadata> result = adapter.headObject("voice/1.mp3");

            assertThat(result).isPresent();
            assertThat(result.get().contentType()).isEqualTo("audio/mpeg");
            assertThat(result.get().sizeBytes()).isEqualTo(1024L);
            assertThat(result.get().durationSeconds()).isEqualTo(42);
            assertThat(result.get().eTag()).isEqualTo("etag-1");
        }

        @Test
        void duration_메타데이터가_숫자가_아니면_null로_처리한다() {
            HeadObjectResponse response = HeadObjectResponse.builder()
                    .contentType("audio/mpeg")
                    .contentLength(1024L)
                    .metadata(Map.of("duration-seconds", "not-a-number"))
                    .build();
            when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(response);

            Optional<StoragePort.ObjectMetadata> result = adapter.headObject("voice/1.mp3");

            assertThat(result).isPresent();
            assertThat(result.get().durationSeconds()).isNull();
        }

        @Test
        void 존재하지_않는_객체는_빈_Optional을_반환한다() {
            when(s3Client.headObject(any(HeadObjectRequest.class)))
                    .thenThrow(NoSuchKeyException.builder().message("no such key").build());

            Optional<StoragePort.ObjectMetadata> result = adapter.headObject("missing/1.mp3");

            assertThat(result).isEmpty();
        }

        @Test
        void HeadObject가_일반_S3_404를_반환해도_빈_Optional을_반환한다() {
            when(s3Client.headObject(any(HeadObjectRequest.class)))
                    .thenThrow(S3Exception.builder().message("not found").statusCode(404).build());

            assertThat(adapter.headObject("missing/1.mp3")).isEmpty();
        }

        @Test
        void S3Exception이_발생하면_그대로_전파한다() {
            when(s3Client.headObject(any(HeadObjectRequest.class)))
                    .thenThrow(S3Exception.builder().message("boom").statusCode(500).build());

            assertThatThrownBy(() -> adapter.headObject("voice/1.mp3"))
                    .isInstanceOf(AwsServiceException.class);
        }
    }

    @Nested
    class 객체_읽기_쓰기_삭제 {

        @Test
        void 객체_바이트를_읽는다() {
            GetObjectResponse response = GetObjectResponse.builder().contentType("image/jpeg").build();
            ResponseBytes<GetObjectResponse> responseBytes =
                    ResponseBytes.fromByteArray(response, new byte[] {1, 2, 3});
            when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

            Optional<StoragePort.StoredContent> result = adapter.getObject("photo/1.jpg");

            assertThat(result).isPresent();
            assertThat(result.get().contentType()).isEqualTo("image/jpeg");
            assertThat(result.get().content()).containsExactly(1, 2, 3);
        }

        @Test
        void 존재하지_않는_객체_읽기는_빈_Optional을_반환한다() {
            when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                    .thenThrow(NoSuchKeyException.builder().message("no such key").build());

            Optional<StoragePort.StoredContent> result = adapter.getObject("missing/1.jpg");

            assertThat(result).isEmpty();
        }

        @Test
        void 객체_읽기에서_일반_S3_404도_빈_Optional을_반환한다() {
            when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                    .thenThrow(S3Exception.builder().message("not found").statusCode(404).build());

            assertThat(adapter.getObject("missing/1.jpg")).isEmpty();
        }

        @Test
        void 객체를_서버_전용_키로_복사한다() {
            adapter.copyObject("response_voice/temporary.aac", "response_voice/confirmed.aac", "etag-1");

            org.mockito.ArgumentCaptor<CopyObjectRequest> captor =
                    org.mockito.ArgumentCaptor.forClass(CopyObjectRequest.class);
            verify(s3Client).copyObject(captor.capture());
            assertThat(captor.getValue().sourceBucket()).isEqualTo("test-bucket");
            assertThat(captor.getValue().sourceKey()).isEqualTo("response_voice/temporary.aac");
            assertThat(captor.getValue().destinationBucket()).isEqualTo("test-bucket");
            assertThat(captor.getValue().destinationKey()).isEqualTo("response_voice/confirmed.aac");
            assertThat(captor.getValue().copySourceIfMatch()).isEqualTo("etag-1");
        }

        @Test
        void 객체를_저장한다() {
            when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            adapter.putObject("photo/1.jpg", "image/jpeg", new byte[] {1, 2, 3});

            verify(s3Client).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
        }

        @Test
        void 객체를_삭제한다() {
            when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                    .thenReturn(DeleteObjectResponse.builder().build());

            adapter.deleteObject("photo/1.jpg");

            verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
        }
    }
}
