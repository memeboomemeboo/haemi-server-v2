package com.memeboo2.haemi.elder.response;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.elder.response.domain.Emotion;
import com.memeboo2.haemi.elder.response.domain.Response;
import com.memeboo2.haemi.elder.response.domain.ResponseType;
import com.memeboo2.haemi.elder.response.domain.TranscriptStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResponseDomainTest {

    private final UUID memoryId = UUID.randomUUID();
    private final UUID elderId = UUID.randomUUID();

    @Test
    void emotion은_1개에서_2개까지_정상_생성된다() {
        Response one = Response.emotion(memoryId, elderId, List.of(Emotion.LOVE));
        Response two = Response.emotion(memoryId, elderId, List.of(Emotion.LOVE, Emotion.HAPPY));

        assertThat(one.getResponseType()).isEqualTo(ResponseType.EMOTION);
        assertThat(one.getEmotions()).containsExactly(Emotion.LOVE);
        assertThat(two.getEmotions()).containsExactly(Emotion.LOVE, Emotion.HAPPY);
    }

    @Test
    void emotion은_비어있으면_INVALID_INPUT을_던진다() {
        assertThatThrownBy(() -> Response.emotion(memoryId, elderId, List.of()))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void emotion은_null이면_INVALID_INPUT을_던진다() {
        assertThatThrownBy(() -> Response.emotion(memoryId, elderId, null))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void emotion은_2개를_초과하면_INVALID_INPUT을_던진다() {
        assertThatThrownBy(() -> Response.emotion(
                memoryId, elderId, List.of(Emotion.LOVE, Emotion.HAPPY, Emotion.JOY)))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void text는_1자에서_100자까지_정상_생성된다() {
        Response response = Response.text(memoryId, elderId, "안녕하세요");

        assertThat(response.getResponseType()).isEqualTo(ResponseType.TEXT);
        assertThat(response.getText()).isEqualTo("안녕하세요");
    }

    @Test
    void text가_공백이면_INVALID_INPUT을_던진다() {
        assertThatThrownBy(() -> Response.text(memoryId, elderId, "  "))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void text가_null이면_INVALID_INPUT을_던진다() {
        assertThatThrownBy(() -> Response.text(memoryId, elderId, null))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void text가_100자를_초과하면_INVALID_INPUT을_던진다() {
        String longText = "가".repeat(101);

        assertThatThrownBy(() -> Response.text(memoryId, elderId, longText))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void image는_정상_생성된다() {
        Response response = Response.image(memoryId, elderId, "media-key");

        assertThat(response.getResponseType()).isEqualTo(ResponseType.IMAGE);
        assertThat(response.getMediaKey()).isEqualTo("media-key");
    }

    @Test
    void voice는_정상_생성된다() {
        Response response = Response.voice(memoryId, elderId, "media-key");

        assertThat(response.getResponseType()).isEqualTo(ResponseType.VOICE);
        assertThat(response.getMediaKey()).isEqualTo("media-key");
        assertThat(response.getTranscriptStatus()).isEqualTo(TranscriptStatus.PENDING);
    }

    @Test
    void 음성_전사를_기록하면_완료_상태가_된다() {
        Response response = Response.voice(memoryId, elderId, "media-key");

        response.recordTranscript("  그 시절이 생각나요  ");

        assertThat(response.getTranscript()).isEqualTo("그 시절이 생각나요");
        assertThat(response.getTranscriptStatus()).isEqualTo(TranscriptStatus.COMPLETED);
    }

    @Test
    void 전사에_실패하면_FAILED가_되고_완료된_전사는_실패로_되돌리지_않는다() {
        Response response = Response.voice(memoryId, elderId, "media-key");

        response.markTranscriptFailed();
        assertThat(response.getTranscriptStatus()).isEqualTo(TranscriptStatus.FAILED);

        response.recordTranscript("전사 성공");
        response.markTranscriptFailed();

        assertThat(response.getTranscriptStatus()).isEqualTo(TranscriptStatus.COMPLETED);
    }

    @Test
    void 비음성_답변에는_전사_실패를_기록할_수_없다() {
        Response response = Response.text(memoryId, elderId, "안녕하세요");

        assertThatThrownBy(response::markTranscriptFailed)
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void 비어있거나_너무_긴_전사는_기록할_수_없다() {
        Response response = Response.voice(memoryId, elderId, "media-key");

        assertThatThrownBy(() -> response.recordTranscript(null))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
        assertThatThrownBy(() -> response.recordTranscript("가".repeat(1001)))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void 비음성_답변에는_전사를_기록할_수_없다() {
        Response response = Response.text(memoryId, elderId, "안녕하세요");

        assertThatThrownBy(() -> response.recordTranscript("전사"))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }
}
