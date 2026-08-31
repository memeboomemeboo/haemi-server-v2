package com.memeboo2.haemi.platform.api;

/**
 * 미디어를 소비하는 기능이 기대하는 업로드 용도.
 *
 * <p>platform/media 내부 도메인 타입을 다른 모듈에 노출하지 않기 위한 공개 계약이다.</p>
 */
public enum MediaPurpose {
    MEMORY_IMAGE,
    GREETING_VOICE,
    RESPONSE_IMAGE,
    RESPONSE_VOICE,
    /**
     * 훈련 세션의 음성 답변. 추억 응답 음성(RESPONSE_VOICE)과 보관 주기·소비 경로가 달라 용도를 분리한다.
     * 전환 기간에는 ConfirmUploadUseCase가 RESPONSE_VOICE로 올라온 기존 클라이언트 업로드도 함께 수용한다. (#144)
     */
    TRAINING_VOICE_ANSWER,
    PROFILE_IMAGE
}
