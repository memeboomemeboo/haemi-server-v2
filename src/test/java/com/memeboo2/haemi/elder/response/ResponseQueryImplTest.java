package com.memeboo2.haemi.elder.response;

import com.memeboo2.haemi.elder.response.application.ResponseQueryImpl;
import com.memeboo2.haemi.elder.response.domain.Emotion;
import com.memeboo2.haemi.elder.response.domain.Response;
import com.memeboo2.haemi.elder.response.infrastructure.ResponseRepository;
import com.memeboo2.haemi.guardian.api.ResponseQuery.ElderResponseActivity;
import com.memeboo2.haemi.guardian.api.ResponseQuery.ResponseItem;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResponseQueryImplTest {

    @Mock ResponseRepository responseRepository;
    @Mock MediaUploadCommand mediaUploadCommand;

    private ResponseQueryImpl responseQuery;

    @BeforeEach
    void setUp() {
        responseQuery = new ResponseQueryImpl(responseRepository, mediaUploadCommand);
        org.mockito.Mockito.lenient().when(mediaUploadCommand.resolveServingUrl(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void findByElderIdBetween은_Response를_ElderResponseActivity로_매핑한다() {
        UUID elderId = UUID.randomUUID();
        UUID memoryId = UUID.randomUUID();
        Instant from = Instant.parse("2026-08-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-02T00:00:00Z");
        Response response = Response.text(memoryId, elderId, "안녕하세요");
        when(responseRepository.findByElderIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(elderId, from, to))
                .thenReturn(List.of(response));

        List<ElderResponseActivity> activities = responseQuery.findByElderIdBetween(elderId, from, to);

        assertThat(activities).hasSize(1);
        ElderResponseActivity activity = activities.get(0);
        assertThat(activity.memoryId()).isEqualTo(memoryId);
        assertThat(activity.responseType()).isEqualTo("TEXT");
        assertThat(activity.text()).isEqualTo("안녕하세요");
        assertThat(activity.transcript()).isNull();
        assertThat(activity.createdAt()).isNull();
    }

    @Test
    void findByMemoryId는_Response를_ResponseItem으로_매핑한다() {
        UUID elderId = UUID.randomUUID();
        UUID memoryId = UUID.randomUUID();
        Response emotionResponse = Response.emotion(memoryId, elderId, List.of(Emotion.LOVE));
        Response textResponse = Response.text(memoryId, elderId, "직접 입력한 댓글");
        Response voiceResponse = Response.voice(memoryId, elderId, "https://media.example/voice.aac", 42);
        voiceResponse.recordTranscript("그 냇가 참 좋았지");
        when(responseRepository.findByMemoryId(memoryId)).thenReturn(List.of(emotionResponse, textResponse, voiceResponse));

        List<ResponseItem> items = responseQuery.findByMemoryId(memoryId);

        assertThat(items).hasSize(3);
        ResponseItem first = items.get(0);
        assertThat(first.responseType()).isEqualTo("EMOTION");
        assertThat(first.emotions()).containsExactly("LOVE");
        assertThat(first.text()).isNull();
        assertThat(first.mediaKey()).isNull();
        assertThat(first.transcriptionStatus()).isEqualTo("NOT_APPLICABLE");

        ResponseItem second = items.get(1);
        assertThat(second.responseType()).isEqualTo("TEXT");
        assertThat(second.text()).isEqualTo("직접 입력한 댓글");

        ResponseItem third = items.get(2);
        assertThat(third.responseType()).isEqualTo("VOICE");
        assertThat(third.emotions()).isEmpty();
        assertThat(third.mediaKey()).isEqualTo("https://media.example/voice.aac");
        assertThat(third.mediaUrl()).isEqualTo("https://media.example/voice.aac");
        assertThat(third.durationSeconds()).isEqualTo(42);
        assertThat(third.text()).isEqualTo("그 냇가 참 좋았지");
        assertThat(third.transcriptionStatus()).isEqualTo("COMPLETED");
    }
}
