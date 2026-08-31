package com.memeboo2.haemi.elder.reminiscence.application;

/** LLM 텍스트 생성 포트. Gemini 어댑터 또는 템플릿 대체 구현이 주입된다. */
public interface AiTextGenerator {

    /**
     * 프롬프트로 텍스트를 생성한다.
     *
     * <p>실패·빈 응답 시 예외 없이 대체 문구를 반환하되, 반환값의 {@code live}에 폴백 여부를 함께 담는다.
     * 텍스트와 live 플래그를 한 반환값으로 묶어, 실제 생성 여부와 저장되는 메타데이터가 구조적으로 어긋나지 않게 한다.
     *
     * @param prompt 생성 프롬프트
     * @return 생성 결과 (텍스트 + 실제 LLM 생성 여부)
     */
    Result generate(String prompt);

    /**
     * 생성 결과.
     *
     * @param text 생성된(또는 대체) 텍스트
     * @param live 실제 LLM 호출로 생성됐으면 {@code true}, 템플릿 대체면 {@code false}
     */
    record Result(String text, boolean live) {
    }
}
