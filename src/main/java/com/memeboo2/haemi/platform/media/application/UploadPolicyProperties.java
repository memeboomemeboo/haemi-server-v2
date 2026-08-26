package com.memeboo2.haemi.platform.media.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "haemi.media")
public record UploadPolicyProperties(
        Image image,
        Voice voice,
        Profile profile,
        PresignedUrl presignedUrl,
        Retention retention
) {

    public record Image(
            @DefaultValue("10485760") long maxSizeBytes,      // 10 MB
            @DefaultValue("4") int memoryMaxCount,
            @DefaultValue("image/jpeg,image/png,image/webp,image/heic,image/heif") List<String> allowedContentTypes
    ) {}

    public record Voice(
            @DefaultValue("12582912") long maxSizeBytes,      // 12 MB
            @DefaultValue("180") int maxDurationSeconds,
            @DefaultValue("audio/aac,audio/mp4,audio/webm,audio/ogg") List<String> allowedContentTypes
    ) {}

    public record Profile(
            @DefaultValue("5242880") long maxSizeBytes,       // 5 MB
            @DefaultValue("image/jpeg,image/png,image/webp") List<String> allowedContentTypes
    ) {}

    public record PresignedUrl(
            @DefaultValue("PT15M") Duration expiry
    ) {}

    public record Retention(
            @DefaultValue("365") int memoryDays,
            @DefaultValue("365") int responseDays
    ) {}
}
