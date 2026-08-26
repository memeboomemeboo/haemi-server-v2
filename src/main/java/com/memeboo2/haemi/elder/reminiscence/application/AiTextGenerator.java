package com.memeboo2.haemi.elder.reminiscence.application;

/** LLM 텍스트 생성 포트. Gemini 어댑터 또는 템플릿 대체 구현이 주입된다. */
public interface AiTextGenerator {

    /**
     * @param prompt 생성 프롬프트
     * @return 생성된 텍스트 (실패 시 빈 문자열이 아니라 예외 없이 대체 문구를 반환하도록 구현)
     */
    String generate(String prompt);

    /** 실제 LLM 호출 여부 (배치 로깅·메타데이터용). */
    boolean isLive();
}
