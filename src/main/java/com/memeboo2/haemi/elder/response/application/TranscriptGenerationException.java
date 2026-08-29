package com.memeboo2.haemi.elder.response.application;

/** 외부 STT 공급자가 전사를 제공하지 못했음을 나타낸다. */
public class TranscriptGenerationException extends RuntimeException {

    public TranscriptGenerationException(String message) {
        super(message);
    }

    public TranscriptGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
