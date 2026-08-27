package com.memeboo2.haemi.platform.media.infrastructure;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.platform.media.application.HeicProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ProcessHeicImageConverter는 OS 프로세스를 직접 실행하므로, 실제 heif-convert 대신
 * 테스트가 생성한 작은 쉘 스크립트를 "command"로 지정해 각 분기를 검증한다.
 */
class ProcessHeicImageConverterTest {

    private final List<Path> createdScripts = new ArrayList<>();

    @AfterEach
    void cleanup() {
        createdScripts.forEach(p -> {
            try {
                Files.deleteIfExists(p);
            } catch (IOException ignored) {
                // best-effort cleanup
            }
        });
    }

    @Test
    void 변환이_비활성화되어_있으면_프로세스를_실행하지_않고_예외를_던진다() {
        HeicProperties props = new HeicProperties(false, "heif-convert", Duration.ofSeconds(20));
        ProcessHeicImageConverter converter = new ProcessHeicImageConverter(props);

        assertThatThrownBy(() -> converter.toJpeg(new byte[]{1, 2, 3}))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.MEDIA_CONVERSION_FAILED));
    }

    @Test
    void 변환_프로세스가_성공하면_출력_바이트를_반환한다() throws IOException {
        // 입력 파일($1)을 그대로 출력 파일($2)로 복사하는 스크립트 — 정상 변환을 흉내낸다.
        Path script = createScript("#!/bin/sh\ncp \"$1\" \"$2\"\nexit 0\n");
        HeicProperties props = new HeicProperties(true, script.toString(), Duration.ofSeconds(5));
        ProcessHeicImageConverter converter = new ProcessHeicImageConverter(props);

        byte[] input = "fake-heic-bytes".getBytes(StandardCharsets.UTF_8);
        byte[] result = converter.toJpeg(input);

        assertThat(result).isEqualTo(input);
    }

    @Test
    void 프로세스가_0이_아닌_종료코드를_반환하면_예외를_던진다() throws IOException {
        Path script = createScript("#!/bin/sh\nexit 1\n");
        HeicProperties props = new HeicProperties(true, script.toString(), Duration.ofSeconds(5));
        ProcessHeicImageConverter converter = new ProcessHeicImageConverter(props);

        assertThatThrownBy(() -> converter.toJpeg(new byte[]{1, 2, 3}))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.MEDIA_CONVERSION_FAILED));
    }

    @Test
    void 출력_파일이_비어있으면_예외를_던진다() throws IOException {
        // 정상 종료(exit 0)하지만 출력 파일에 아무것도 쓰지 않는 스크립트.
        Path script = createScript("#!/bin/sh\nexit 0\n");
        HeicProperties props = new HeicProperties(true, script.toString(), Duration.ofSeconds(5));
        ProcessHeicImageConverter converter = new ProcessHeicImageConverter(props);

        assertThatThrownBy(() -> converter.toJpeg(new byte[]{1, 2, 3}))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.MEDIA_CONVERSION_FAILED));
    }

    @Test
    void 프로세스가_타임아웃을_초과하면_강제종료하고_예외를_던진다() throws IOException {
        Path script = createScript("#!/bin/sh\nsleep 5\n");
        HeicProperties props = new HeicProperties(true, script.toString(), Duration.ofMillis(200));
        ProcessHeicImageConverter converter = new ProcessHeicImageConverter(props);

        assertThatThrownBy(() -> converter.toJpeg(new byte[]{1, 2, 3}))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.MEDIA_CONVERSION_FAILED));
    }

    @Test
    void 존재하지_않는_명령이면_IOException을_감싸_예외를_던진다() {
        HeicProperties props = new HeicProperties(true, "/no/such/binary-xyz", Duration.ofSeconds(5));
        ProcessHeicImageConverter converter = new ProcessHeicImageConverter(props);

        assertThatThrownBy(() -> converter.toJpeg(new byte[]{1, 2, 3}))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.MEDIA_CONVERSION_FAILED));
    }

    private Path createScript(String content) throws IOException {
        Path script = Files.createTempFile("heic-test-", ".sh");
        Files.writeString(script, content);
        Files.setPosixFilePermissions(script, Set.of(
                java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                java.nio.file.attribute.PosixFilePermission.OWNER_WRITE,
                java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE));
        createdScripts.add(script);
        return script;
    }
}
