package com.memeboo2.haemi.elder.response.presentation.dto;

import com.memeboo2.haemi.elder.response.domain.Emotion;
import com.memeboo2.haemi.elder.response.domain.Response;
import com.memeboo2.haemi.elder.response.domain.ResponseType;
import com.memeboo2.haemi.elder.response.domain.TranscriptStatus;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ResponseSummary(
        UUID id,
        ResponseType responseType,
        List<Emotion> emotions,
        String text,
        String transcript,
        TranscriptStatus transcriptionStatus,
        String mediaKey,
        Integer durationSeconds,
        Instant createdAt
) {
    public static ResponseSummary from(Response r) {
        return from(r, null);
    }

    public static ResponseSummary from(Response r, MediaUploadCommand mediaUploadCommand) {
        return new ResponseSummary(
                r.getId(), r.getResponseType(),
                r.getEmotions().isEmpty() ? null : r.getEmotions(),
                r.getText(), r.getTranscript(), r.getTranscriptStatus(),
                mediaUploadCommand == null ? r.getMediaKey() : mediaUploadCommand.resolveServingUrl(r.getMediaKey()),
                r.getDurationSeconds(), r.getCreatedAt()
        );
    }
}
