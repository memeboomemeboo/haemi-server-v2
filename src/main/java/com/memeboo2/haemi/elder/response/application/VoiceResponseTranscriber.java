package com.memeboo2.haemi.elder.response.application;

/** 확정된 음성 답변을 텍스트로 전사하는 외부 공급자 포트. */
public interface VoiceResponseTranscriber {

    /**
     * @throws TranscriptGenerationException 공급자 미설정, 요청 실패 또는 유효하지 않은 전사 결과일 때
     */
    String transcribe(String contentType, byte[] audio);
}
