package com.memeboo2.haemi.elder.response.presentation;

import com.memeboo2.haemi.elder.response.domain.Response;
import com.memeboo2.haemi.elder.response.presentation.dto.ResponseSummary;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResponseSummaryTest {

    @Test
    void media_storage_key를_serving_URL로_변환한다() {
        Response response = mock(Response.class);
        when(response.getMediaKey()).thenReturn("response_voice/voice.aac");
        MediaUploadCommand media = mock(MediaUploadCommand.class);
        when(media.resolveServingUrl("response_voice/voice.aac")).thenReturn("https://cdn.example/voice.aac");

        ResponseSummary summary = ResponseSummary.from(response, media);

        assertThat(summary.mediaKey()).isEqualTo("https://cdn.example/voice.aac");
    }
}
