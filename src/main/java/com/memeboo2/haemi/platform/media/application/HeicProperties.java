package com.memeboo2.haemi.platform.media.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * HEIC 서버 변환 설정.
 *
 * <p>{@code command}는 {@code <command> <입력파일> <출력파일>} 형태로 실행된다.
 * 기본값 {@code heif-convert}(libheif-tools). ImageMagick을 쓰려면 {@code magick}으로 바꾼다.
 */
@ConfigurationProperties(prefix = "haemi.media.heic")
public record HeicProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("heif-convert") String command,
        @DefaultValue("PT20S") Duration timeout
) {
}
