package com.memeboo2.haemi.platform.media.infrastructure;

import com.memeboo2.haemi.platform.media.domain.MediaType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
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
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * 실 스토리지(S3/R2) 어댑터. prod 프로파일에서 {@link LocalStorageAdapter}를 대체한다.
 *
 * <p>업로드는 presigned PUT URL로 클라이언트가 직접 수행하고, 확정(confirm) 시 서버가
 * {@code headObject}로 실제 메타데이터를 검증한다. 서빙은 만료가 있는 presigned GET URL을 발급한다.
 *
 * <p>음성 길이(duration)는 S3가 알 수 없으므로 클라이언트가 업로드 시 사용자 메타데이터
 * {@code x-amz-meta-duration-seconds}를 넣으면 이를 읽어 검증에 사용한다.
 */
@Component
@Profile("prod")
class S3StorageAdapter implements StoragePort {

    private static final String DURATION_METADATA_KEY = "duration-seconds";

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final S3StorageProperties props;

    S3StorageAdapter(S3Client s3Client, S3Presigner presigner, S3StorageProperties props) {
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.props = props;
    }

    @Override
    public URI generatePresignedPutUrl(String storageKey, String contentType, long expirySeconds,
                                       Integer expectedDurationSeconds) {
        PutObjectRequest.Builder putBuilder = PutObjectRequest.builder()
                .bucket(props.bucket())
                .key(storageKey)
                .contentType(contentType);
        if (expectedDurationSeconds != null) {
            putBuilder.metadata(java.util.Map.of(DURATION_METADATA_KEY, String.valueOf(expectedDurationSeconds)));
        }

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expirySeconds))
                .putObjectRequest(putBuilder.build())
                .build();

        return URI.create(presigner.presignPutObject(presignRequest).url().toString());
    }

    @Override
    public URI generateServingUrl(String storageKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(props.bucket())
                .key(storageKey)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(props.servingUrlExpiry())
                .getObjectRequest(getObjectRequest)
                .build();
        return URI.create(presigner.presignGetObject(presignRequest).url().toString());
    }

    @Override
    public String buildStorageKey(MediaType mediaType, String originalFilename) {
        String ext = extractExtension(originalFilename);
        return mediaType.name().toLowerCase() + "/" + UUID.randomUUID() + ext;
    }

    @Override
    public Optional<ObjectMetadata> headObject(String storageKey) {
        try {
            HeadObjectResponse head = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(props.bucket())
                    .key(storageKey)
                    .build());
            Integer duration = parseDuration(head.metadata().get(DURATION_METADATA_KEY));
            return Optional.of(new ObjectMetadata(head.contentType(), head.contentLength(), duration));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<StoredContent> getObject(String storageKey) {
        try {
            ResponseBytes<GetObjectResponse> object = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(props.bucket())
                    .key(storageKey)
                    .build());
            return Optional.of(new StoredContent(object.response().contentType(), object.asByteArray()));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        }
    }

    @Override
    public void putObject(String storageKey, String contentType, byte[] content) {
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(props.bucket())
                        .key(storageKey)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(content));
    }

    @Override
    public void deleteObject(String storageKey) {
        s3Client.deleteObject(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder()
                .bucket(props.bucket())
                .key(storageKey)
                .build());
    }

    private Integer parseDuration(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }
}
