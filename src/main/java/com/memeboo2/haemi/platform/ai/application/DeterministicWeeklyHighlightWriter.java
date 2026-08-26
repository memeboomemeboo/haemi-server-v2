package com.memeboo2.haemi.platform.ai.application;

import com.memeboo2.haemi.platform.ai.api.WeeklyHighlightFact;
import com.memeboo2.haemi.platform.ai.api.WeeklyHighlightPrompt;
import com.memeboo2.haemi.platform.ai.api.WeeklyHighlightWriter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 외부 AI 설정 전에도 안전한 결과를 보장하는 RPT-ATT-005 기본 구현.
 *
 * <p>입력이 열거형 사실로 한정돼 있으므로 점수·정답률·진단명·비교 결과를 문구에 섞을 수 없다.
 * 나중에 외부 모델 어댑터를 추가하더라도 {@link WeeklyHighlightWriter}만 교체하면 된다.</p>
 */
@Component
public class DeterministicWeeklyHighlightWriter implements WeeklyHighlightWriter {

    private static final Map<WeeklyHighlightFact, String> STRENGTH_LINES = Map.of(
            WeeklyHighlightFact.ORIENTATION_STRENGTH, "날짜와 요일 감각을 잘 이어가고 계세요.",
            WeeklyHighlightFact.RECALL_STRENGTH, "옛 기억을 떠올리는 시간을 잘 이어가고 계세요.",
            WeeklyHighlightFact.LANGUAGE_STRENGTH, "말로 표현하는 시간을 잘 이어가고 계세요.",
            WeeklyHighlightFact.DELAYED_RECALL_STRENGTH, "앞서 본 내용을 다시 떠올리는 힘을 잘 보여주셨어요."
    );

    private static final Map<WeeklyHighlightFact, String> OBSERVATION_LINES = Map.of(
            WeeklyHighlightFact.ORIENTATION_SUPPORT, "날짜와 요일은 요즘 조금 어려워하실 수 있어요.",
            WeeklyHighlightFact.RECALL_SUPPORT, "옛 기억을 떠올리는 일은 요즘 조금 어려워하실 수 있어요.",
            WeeklyHighlightFact.LANGUAGE_SUPPORT, "말로 표현하는 일은 요즘 조금 어려워하실 수 있어요.",
            WeeklyHighlightFact.DELAYED_RECALL_SUPPORT, "앞서 본 내용을 다시 떠올리는 일은 요즘 조금 어려워하실 수 있어요."
    );

    @Override
    public List<String> write(WeeklyHighlightPrompt prompt) {
        List<String> lines = new ArrayList<>(3);
        if (prompt.weeklyParticipationDays() > 0) {
            lines.add("이번 주 " + prompt.weeklyParticipationDays() + "일 참여하셨어요.");
        }

        addFirstMappedLine(lines, prompt.strengths(), STRENGTH_LINES);
        if (lines.isEmpty()) {
            lines.add("다음 주에는 부담 없이 인지 훈련을 시작해 보세요.");
        }
        addFirstMappedLine(lines, prompt.observations(), OBSERVATION_LINES);

        return List.copyOf(lines);
    }

    private void addFirstMappedLine(
            List<String> lines,
            List<WeeklyHighlightFact> facts,
            Map<WeeklyHighlightFact, String> templates
    ) {
        if (lines.size() >= 3) {
            return;
        }
        facts.stream()
                .map(templates::get)
                .filter(Objects::nonNull)
                .findFirst()
                .ifPresent(lines::add);
    }
}
