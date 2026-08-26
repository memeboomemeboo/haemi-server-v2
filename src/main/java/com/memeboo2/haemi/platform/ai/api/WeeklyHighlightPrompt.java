package com.memeboo2.haemi.platform.ai.api;

import java.util.List;

/**
 * RPT-ATT-005 문구 생성 입력.
 *
 * <p>주간 참여일은 기능 명세에서 허용된 참여 게이지이며, 인지 점수나 정답률은 전달하지 않는다.</p>
 */
public record WeeklyHighlightPrompt(
        int weeklyParticipationDays,
        List<WeeklyHighlightFact> strengths,
        List<WeeklyHighlightFact> observations
) {

    public WeeklyHighlightPrompt {
        if (weeklyParticipationDays < 0 || weeklyParticipationDays > 7) {
            throw new IllegalArgumentException("weeklyParticipationDays must be between 0 and 7");
        }
        strengths = List.copyOf(strengths);
        observations = List.copyOf(observations);
        if (strengths.stream().anyMatch(fact -> !fact.isStrength())) {
            throw new IllegalArgumentException("strengths must contain only strength facts");
        }
        if (observations.stream().anyMatch(fact -> !fact.isObservation())) {
            throw new IllegalArgumentException("observations must contain only support facts");
        }
    }
}
