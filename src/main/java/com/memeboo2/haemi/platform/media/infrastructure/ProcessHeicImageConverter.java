package com.memeboo2.haemi.platform.media.infrastructure;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.platform.media.application.HeicImageConverter;
import com.memeboo2.haemi.platform.media.application.HeicProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 네이티브 바이너리로 HEIC→JPEG 변환 (동기). {@code <command> <입력> <출력>} 형식으로 호출한다.
 * 기본 바이너리는 libheif-tools의 {@code heif-convert}.
 */
@Component
public class ProcessHeicImageConverter implements HeicImageConverter {

    private static final Logger log = LoggerFactory.getLogger(ProcessHeicImageConverter.class);

    private final HeicProperties props;

    public ProcessHeicImageConverter(HeicProperties props) {
        this.props = props;
    }

    @Override
    public byte[] toJpeg(byte[] heic) {
        if (!props.enabled()) {
            throw new DomainException(ErrorCode.MEDIA_CONVERSION_FAILED, "HEIC 변환이 비활성화되어 있습니다.");
        }
        Path in = null;
        Path out = null;
        try {
            in = Files.createTempFile("haemi-heic-", ".heic");
            out = Files.createTempFile("haemi-heic-", ".jpg");
            Files.write(in, heic);

            List<String> command = List.of(props.command(), in.toString(), out.toString());
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();

            boolean finished = process.waitFor(props.timeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new DomainException(ErrorCode.MEDIA_CONVERSION_FAILED, "HEIC 변환이 시간 초과되었습니다.");
            }
            if (process.exitValue() != 0) {
                String output = new String(process.getInputStream().readAllBytes());
                log.warn("HEIC 변환 실패 exit={} output={}", process.exitValue(), output);
                throw new DomainException(ErrorCode.MEDIA_CONVERSION_FAILED, "HEIC 변환에 실패했습니다.");
            }
            byte[] jpeg = Files.readAllBytes(out);
            if (jpeg.length == 0) {
                throw new DomainException(ErrorCode.MEDIA_CONVERSION_FAILED, "변환된 이미지가 비어 있습니다.");
            }
            return jpeg;
        } catch (IOException e) {
            throw new DomainException(ErrorCode.MEDIA_CONVERSION_FAILED, "HEIC 변환 중 오류가 발생했습니다.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DomainException(ErrorCode.MEDIA_CONVERSION_FAILED, "HEIC 변환이 중단되었습니다.");
        } finally {
            deleteQuietly(in);
            deleteQuietly(out);
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.debug("임시 파일 삭제 실패: {}", path);
        }
    }
}
