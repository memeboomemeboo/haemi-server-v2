package com.memeboo2.haemi.elder.response;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.elder.response.application.CreateResponseUseCase;
import com.memeboo2.haemi.common.event.ElderResponded;
import com.memeboo2.haemi.elder.response.domain.Emotion;
import com.memeboo2.haemi.elder.response.domain.Response;
import com.memeboo2.haemi.elder.response.domain.ResponseType;
import com.memeboo2.haemi.elder.response.infrastructure.ResponseRepository;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateResponseUseCaseTest {

    @Mock ResponseRepository responseRepository;
    @Mock MediaUploadCommand mediaUploadCommand;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks CreateResponseUseCase useCase;

    UUID elderId  = UUID.randomUUID();
    UUID memoryId = UUID.randomUUID();

    @Test
    void 마음전하기_정상() {
        given(responseRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        useCase.emotion(elderId, memoryId, List.of(Emotion.LOVE, Emotion.HAPPY));

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(responseRepository).save(captor.capture());
        assertThat(captor.getValue().getResponseType()).isEqualTo(ResponseType.EMOTION);
        assertThat(captor.getValue().getEmotions()).containsExactly(Emotion.LOVE, Emotion.HAPPY);

        ArgumentCaptor<ElderResponded> eventCaptor = ArgumentCaptor.forClass(ElderResponded.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().memoryId()).isEqualTo(memoryId);
    }

    @Test
    void 마음전하기_3개_초과는_400() {
        assertThatThrownBy(() -> useCase.emotion(elderId, memoryId,
                List.of(Emotion.LOVE, Emotion.HAPPY, Emotion.JOY)))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void 텍스트_댓글_정상() {
        given(responseRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        useCase.text(elderId, memoryId, "좋아요!");

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(responseRepository).save(captor.capture());
        assertThat(captor.getValue().getResponseType()).isEqualTo(ResponseType.TEXT);
        assertThat(captor.getValue().getText()).isEqualTo("좋아요!");
    }

    @Test
    void 텍스트_100자_초과는_400() {
        String over = "a".repeat(101);
        assertThatThrownBy(() -> useCase.text(elderId, memoryId, over))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void 음성_정상() {
        UUID refId = UUID.randomUUID();
        given(mediaUploadCommand.confirmUpload(elderId, refId))
                .willReturn(URI.create("http://localhost/serve?key=voice.aac"));
        given(responseRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        useCase.voice(elderId, memoryId, refId);

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(responseRepository).save(captor.capture());
        assertThat(captor.getValue().getResponseType()).isEqualTo(ResponseType.VOICE);
        assertThat(captor.getValue().getMediaKey()).contains("voice.aac");
    }
}
