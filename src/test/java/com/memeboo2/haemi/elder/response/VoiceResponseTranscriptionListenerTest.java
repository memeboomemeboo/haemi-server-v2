package com.memeboo2.haemi.elder.response;

import com.memeboo2.haemi.common.event.VoiceResponseCreated;
import com.memeboo2.haemi.elder.response.application.TranscriptGenerationException;
import com.memeboo2.haemi.elder.response.application.VoiceResponseTranscriber;
import com.memeboo2.haemi.elder.response.application.VoiceResponseTranscriptionListener;
import com.memeboo2.haemi.elder.response.domain.Response;
import com.memeboo2.haemi.elder.response.domain.TranscriptStatus;
import com.memeboo2.haemi.elder.response.infrastructure.ResponseRepository;
import com.memeboo2.haemi.platform.api.MediaPurpose;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class VoiceResponseTranscriptionListenerTest {

    @Mock ResponseRepository responseRepository;
    @Mock MediaUploadCommand mediaUploadCommand;
    @Mock VoiceResponseTranscriber transcriber;

    private VoiceResponseTranscriptionListener listener;
    private UUID responseId;
    private UUID mediaRefId;
    private Response response;

    @BeforeEach
    void setUp() {
        listener = new VoiceResponseTranscriptionListener(responseRepository, mediaUploadCommand, transcriber);
        responseId = UUID.randomUUID();
        mediaRefId = UUID.randomUUID();
        response = Response.voice(UUID.randomUUID(), UUID.randomUUID(), "https://media.example/voice.aac", 12);
        ReflectionTestUtils.setField(response, "id", responseId);
        lenient().when(responseRepository.findById(responseId)).thenReturn(Optional.of(response));
    }

    @Test
    void 확정된_음성을_Gemini에_전송한_결과를_저장한다() {
        given(mediaUploadCommand.readConfirmedMedia(mediaRefId, MediaPurpose.RESPONSE_VOICE))
                .willReturn(Optional.of(new MediaUploadCommand.ConfirmedMedia("audio/aac", new byte[]{1, 2})));
        given(transcriber.transcribe(eq("audio/aac"), any(byte[].class))).willReturn("  추억이 나네요  ");

        listener.on(new VoiceResponseCreated(responseId, mediaRefId));

        assertThat(response.getTranscript()).isEqualTo("추억이 나네요");
        assertThat(response.getTranscriptStatus()).isEqualTo(TranscriptStatus.COMPLETED);
        verify(responseRepository).save(response);
    }

    @Test
    void Gemini_전사_실패는_응답_생성을_되돌리지_않고_FAILED로_저장한다() {
        given(mediaUploadCommand.readConfirmedMedia(mediaRefId, MediaPurpose.RESPONSE_VOICE))
                .willReturn(Optional.of(new MediaUploadCommand.ConfirmedMedia("audio/aac", new byte[]{1, 2})));
        given(transcriber.transcribe(any(), any())).willThrow(new TranscriptGenerationException("키 없음"));

        listener.on(new VoiceResponseCreated(responseId, mediaRefId));

        assertThat(response.getTranscript()).isNull();
        assertThat(response.getTranscriptStatus()).isEqualTo(TranscriptStatus.FAILED);
        verify(responseRepository).save(response);
    }

    @Test
    void 확정된_원본을_읽을_수_없어도_FAILED로_저장한다() {
        given(mediaUploadCommand.readConfirmedMedia(mediaRefId, MediaPurpose.RESPONSE_VOICE))
                .willReturn(Optional.empty());

        listener.on(new VoiceResponseCreated(responseId, mediaRefId));

        assertThat(response.getTranscriptStatus()).isEqualTo(TranscriptStatus.FAILED);
        verifyNoInteractions(transcriber);
        verify(responseRepository).save(response);
    }

    @Test
    void 이미_완료된_전사는_이벤트가_중복되어도_다시_호출하지_않는다() {
        response.recordTranscript("이미 전사됨");

        listener.on(new VoiceResponseCreated(responseId, mediaRefId));

        assertThat(response.getTranscriptStatus()).isEqualTo(TranscriptStatus.COMPLETED);
        verifyNoInteractions(mediaUploadCommand, transcriber);
    }

    @Test
    void 삭제된_응답의_재전사_이벤트는_무시한다() {
        UUID deletedResponseId = UUID.randomUUID();
        given(responseRepository.findById(deletedResponseId)).willReturn(Optional.empty());

        listener.on(new VoiceResponseCreated(deletedResponseId, mediaRefId));

        verifyNoInteractions(mediaUploadCommand, transcriber);
    }

    @Test
    void 전사_리스너는_Gemini_호출_동안_DB_트랜잭션을_열지_않는다() throws NoSuchMethodException {
        ApplicationModuleListener annotation = VoiceResponseTranscriptionListener.class
                .getMethod("on", VoiceResponseCreated.class)
                .getAnnotation(ApplicationModuleListener.class);

        assertThat(annotation.propagation()).isEqualTo(Propagation.NOT_SUPPORTED);
    }
}
