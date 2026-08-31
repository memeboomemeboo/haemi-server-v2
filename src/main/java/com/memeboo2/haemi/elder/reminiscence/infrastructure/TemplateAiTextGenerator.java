package com.memeboo2.haemi.elder.reminiscence.infrastructure;

import com.memeboo2.haemi.elder.reminiscence.application.AiTextGenerator;

/**
 * API 키가 없거나 호출 실패 시 사용하는 템플릿 대체 생성기.
 * 데모/로컬에서 파이프라인이 끊기지 않도록 프롬프트와 무관한 일반 회상 문구를 반환한다.
 */
class TemplateAiTextGenerator implements AiTextGenerator {

    private static final String TEMPLATE = """
            오늘은 지난 시절의 따뜻한 기억을 떠올려 보는 건 어떨까요?
            젊은 날 즐겨 듣던 노래나 가족과 함께한 명절의 풍경을 하나씩 되새겨 보세요.
            작은 기억 하나가 오늘 하루를 환하게 밝혀 줄 거예요.""";

    @Override
    public Result generate(String prompt) {
        return new Result(TEMPLATE, false);
    }
}
