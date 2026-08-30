package com.memeboo2.haemi.elder.response.application;

import com.memeboo2.haemi.common.event.VoiceResponseCreated;
import com.memeboo2.haemi.elder.response.domain.Response;
import com.memeboo2.haemi.elder.response.domain.ResponseType;
import com.memeboo2.haemi.elder.response.domain.TranscriptStatus;
import com.memeboo2.haemi.elder.response.infrastructure.ResponseRepository;
import com.memeboo2.haemi.platform.api.MediaPurpose;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;

/** 음성 답변이 커밋된 뒤 Gemini 전사를 실행하고 결과 상태만 갱신한다. */
@Component
@RequiredArgsConstructor
public class VoiceResponseTranscriptionListener {

    private static final Logger log = LoggerFactory.getLogger(VoiceResponseTranscriptionListener.class);

    private final ResponseRepository responseRepository;
    private final MediaUploadCommand mediaUploadCommand;
    private final VoiceResponseTranscriber transcriber;

    /**
     * Gemini 대기와 호출은 DB 트랜잭션 밖에서 수행한다.
     * 상태 조회·갱신은 각 repository 호출의 짧은 트랜잭션으로 끝나야 대기 작업이 커넥션을 점유하지 않는다.
     */
    @ApplicationModuleListener(propagation = Propagation.NOT_SUPPORTED)
    public void on(VoiceResponseCreated event) {
        responseRepository.findById(event.responseId()).ifPresent(response -> transcribe(response, event));
    }

    private void transcribe(Response response, VoiceResponseCreated event) {
        if (response.getResponseType() != ResponseType.VOICE
                || response.getTranscriptStatus() != TranscriptStatus.PENDING) {
            return;
        }
        try {
            MediaUploadCommand.ConfirmedMedia media = mediaUploadCommand
                    .readConfirmedMedia(event.mediaRefId(), MediaPurpose.RESPONSE_VOICE)
                    .orElseThrow(() -> new TranscriptGenerationException("확정된 음성 원본을 찾을 수 없습니다."));
            response.recordTranscript(transcriber.transcribe(media.contentType(), media.content()));
            responseRepository.save(response);
            log.info("음성 답변 전사 완료: responseId={}", event.responseId());
        } catch (RuntimeException e) {
            response.markTranscriptFailed();
            responseRepository.save(response);
            // 음성 데이터·전사 문구·API 키는 로그에 남기지 않는다.
            log.warn("음성 답변 전사 실패: responseId={}, cause={}", event.responseId(), e.toString());
        }
    }
}
