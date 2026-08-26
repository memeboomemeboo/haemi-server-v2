package com.memeboo2.haemi.guardian.report.application;

import com.memeboo2.haemi.guardian.report.api.CognitiveStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** RPT-ATT-004의 7일 정답률 및 4주 하락 판정 규칙. */
@Component
@RequiredArgsConstructor
public class CognitiveStatusCalculator {

    private final ReportProperties properties;

    public CognitiveStatus status(int scoredAnswerCount, int correctAnswerCount, boolean fourWeekDecline) {
        if (scoredAnswerCount == 0) {
            return CognitiveStatus.NOT_AVAILABLE;
        }
        if (fourWeekDecline || accuracyPercent(correctAnswerCount, scoredAnswerCount)
                < properties.cognitiveNormalAccuracyPercent()) {
            return CognitiveStatus.WATCH;
        }
        if (accuracyPercent(correctAnswerCount, scoredAnswerCount) >= properties.cognitiveGoodAccuracyPercent()) {
            return CognitiveStatus.GOOD;
        }
        return CognitiveStatus.NORMAL;
    }

    public boolean strictlyDeclines(int[] scoredCounts, int[] correctCounts) {
        if (scoredCounts.length != properties.cognitiveTrendWindowWeeks()
                || correctCounts.length != properties.cognitiveTrendWindowWeeks()) {
            throw new IllegalArgumentException("추세 판정은 설정된 주 수만큼의 집계가 필요합니다.");
        }
        for (int index = 0; index < scoredCounts.length; index++) {
            if (scoredCounts[index] == 0) {
                return false;
            }
        }
        for (int index = 1; index < scoredCounts.length; index++) {
            if ((long) correctCounts[index - 1] * scoredCounts[index]
                    <= (long) correctCounts[index] * scoredCounts[index - 1]) {
                return false;
            }
        }
        return true;
    }

    private int accuracyPercent(int correctAnswerCount, int scoredAnswerCount) {
        return correctAnswerCount * 100 / scoredAnswerCount;
    }
}
