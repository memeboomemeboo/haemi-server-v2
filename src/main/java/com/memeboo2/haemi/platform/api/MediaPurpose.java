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
    PROFILE_IMAGE
}
