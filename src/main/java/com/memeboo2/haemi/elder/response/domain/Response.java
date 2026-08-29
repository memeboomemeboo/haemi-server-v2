package com.memeboo2.haemi.elder.response.domain;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "elder_responses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Response extends BaseEntity {

    private static final int MAX_TEXT_LENGTH = 100;
    private static final int MAX_EMOTIONS = 2;

    /** 대상 추억 (FK 없음 — 모듈 간 FK 금지) */
    @Column(nullable = false)
    private UUID memoryId;

    /** 답변 어르신 (FK 없음) */
    @Column(nullable = false)
    private UUID elderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResponseType responseType;

    /** 마음 전하기 감정 (EMOTION 타입일 때 사용) */
    @ElementCollection
    @CollectionTable(name = "elder_response_emotions", joinColumns = @JoinColumn(name = "response_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "emotion")
    private List<Emotion> emotions = new ArrayList<>();

    /** 텍스트 댓글 (TEXT 타입) */
    @Column(length = 100)
    private String text;

    /** 미디어 키 (IMAGE / VOICE 타입) */
    @Column(length = 500)
    private String mediaKey;

    /** 음성 답변 재생 시간(초). VOICE 타입 외에는 null. */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /** 음성 답변 전사(STT) 텍스트. 아직 전사되지 않았거나 음성 타입이 아니면 null (#100 X3) */
    @Column(name = "transcript", length = 1000)
    private String transcript;

    /** 음성 전사 상태. 비음성 응답은 NOT_APPLICABLE이다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "transcript_status", nullable = false, length = 20)
    private TranscriptStatus transcriptStatus;

    public static Response emotion(UUID memoryId, UUID elderId, List<Emotion> emotions) {
        if (emotions == null || emotions.isEmpty() || emotions.size() > MAX_EMOTIONS) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "감정은 1~" + MAX_EMOTIONS + "개 선택해야 합니다.");
        }
        Response r = new Response();
        r.memoryId = memoryId;
        r.elderId = elderId;
        r.responseType = ResponseType.EMOTION;
        r.transcriptStatus = TranscriptStatus.NOT_APPLICABLE;
        r.emotions.addAll(emotions);
        return r;
    }

    public static Response text(UUID memoryId, UUID elderId, String text) {
        if (text == null || text.isBlank() || text.length() > MAX_TEXT_LENGTH) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "댓글은 1~" + MAX_TEXT_LENGTH + "자입니다.");
        }
        Response r = new Response();
        r.memoryId = memoryId;
        r.elderId = elderId;
        r.responseType = ResponseType.TEXT;
        r.transcriptStatus = TranscriptStatus.NOT_APPLICABLE;
        r.text = text;
        return r;
    }

    public static Response image(UUID memoryId, UUID elderId, String mediaKey) {
        Response r = new Response();
        r.memoryId = memoryId;
        r.elderId = elderId;
        r.responseType = ResponseType.IMAGE;
        r.transcriptStatus = TranscriptStatus.NOT_APPLICABLE;
        r.mediaKey = mediaKey;
        return r;
    }

    public static Response voice(UUID memoryId, UUID elderId, String mediaKey) {
        return voice(memoryId, elderId, mediaKey, null);
    }

    public static Response voice(UUID memoryId, UUID elderId, String mediaKey, Integer durationSeconds) {
        Response r = new Response();
        r.memoryId = memoryId;
        r.elderId = elderId;
        r.responseType = ResponseType.VOICE;
        r.transcriptStatus = TranscriptStatus.PENDING;
        r.mediaKey = mediaKey;
        r.durationSeconds = durationSeconds;
        return r;
    }

    /** 비동기 STT 작업이 확정한 음성 전사를 저장한다. 전사 전에는 null을 유지한다. */
    public void recordTranscript(String transcript) {
        if (responseType != ResponseType.VOICE) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "음성 답변에만 전사를 저장할 수 있습니다.");
        }
        String normalized = transcript == null ? null : transcript.strip();
        if (normalized == null || normalized.isBlank() || normalized.length() > 1000) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "음성 전사는 1~1000자여야 합니다.");
        }
        this.transcript = normalized;
        this.transcriptStatus = TranscriptStatus.COMPLETED;
    }

    /** 복구 가능한 외부 STT 실패를 기록한다. 실패 원문은 개인정보·공급자 정보를 노출하지 않기 위해 저장하지 않는다. */
    public void markTranscriptFailed() {
        if (responseType != ResponseType.VOICE) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "음성 답변에만 전사 실패를 기록할 수 있습니다.");
        }
        if (transcriptStatus != TranscriptStatus.COMPLETED) {
            this.transcriptStatus = TranscriptStatus.FAILED;
        }
    }

    public List<Emotion> getEmotions() {
        return Collections.unmodifiableList(emotions);
    }
}
