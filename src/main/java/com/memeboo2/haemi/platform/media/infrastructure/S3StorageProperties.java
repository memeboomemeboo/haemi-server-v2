package com.memeboo2.haemi.platform.media.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * 실 스토리지(S3/R2) 접속 설정. prod 프로파일에서 {@link S3StorageAdapter}가 사용한다.
 *
 * <p>access-key/secret-key가 비어 있으면 AWS 기본 자격증명 체인(EC2 인스턴스 역할 등)을 따른다.
 * endpoint를 지정하면 Cloudflare R2 등 S3 호환 스토리지에 붙을 수 있다.
 */
@ConfigurationProperties(prefix = "haemi.media.storage")
public record S3StorageProperties(
        String bucket,
        @DefaultValue("ap-northeast-2") String region,
        String endpoint,
        String accessKey,
        String secretKey,
        @DefaultValue("false") boolean pathStyleAccess,
        @DefaultValue("PT15M") Duration presignedPutExpiry,
        @DefaultValue("PT1H") Duration servingUrlExpiry
) {
}
