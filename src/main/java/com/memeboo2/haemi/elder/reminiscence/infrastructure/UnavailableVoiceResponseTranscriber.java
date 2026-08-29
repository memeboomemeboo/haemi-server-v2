package com.memeboo2.haemi.elder.reminiscence.infrastructure;

import com.memeboo2.haemi.elder.response.application.TranscriptGenerationException;
import com.memeboo2.haemi.elder.response.application.VoiceResponseTranscriber;

/** API 키가 없는 환경에서 음성 내용을 임의 텍스트로 대체하지 않기 위한 안전한 구현체. */
class UnavailableVoiceResponseTranscriber implements VoiceResponseTranscriber {

    @Override
    public String transcribe(String contentType, byte[] audio) {
        throw new TranscriptGenerationException("Gemini API 키가 설정되지 않았습니다.");
    }
}
