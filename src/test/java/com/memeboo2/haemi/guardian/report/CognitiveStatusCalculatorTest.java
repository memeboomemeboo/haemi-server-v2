package com.memeboo2.haemi.guardian.report;

import com.memeboo2.haemi.guardian.report.api.CognitiveStatus;
import com.memeboo2.haemi.guardian.report.application.CognitiveStatusCalculator;
import com.memeboo2.haemi.guardian.report.application.ReportProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CognitiveStatusCalculatorTest {

    private final CognitiveStatusCalculator calculator = new CognitiveStatusCalculator(
            new ReportProperties(5, 3, 7, 4, 70, 40, 7, 4));

    @Test
    void 최근_7일_정답률_기준으로_3색을_판정한다() {
        assertThat(calculator.status(10, 7, false)).isEqualTo(CognitiveStatus.GOOD);
        assertThat(calculator.status(10, 4, false)).isEqualTo(CognitiveStatus.NORMAL);
        assertThat(calculator.status(10, 3, false)).isEqualTo(CognitiveStatus.WATCH);
    }

    @Test
    void 자동_채점_응답이_없으면_색상을_억지로_정하지_않는다() {
        assertThat(calculator.status(0, 0, false)).isEqualTo(CognitiveStatus.NOT_AVAILABLE);
    }

    @Test
    void 네_주의_정답률이_엄격하게_연속_하락하면_관찰필요다() {
        assertThat(calculator.strictlyDeclines(
                new int[]{10, 10, 10, 10}, new int[]{9, 7, 5, 3})).isTrue();
        assertThat(calculator.status(10, 8, true)).isEqualTo(CognitiveStatus.WATCH);
    }

    @Test
    void 동률이거나_채점_데이터가_없는_주는_연속_하락이_아니다() {
        assertThat(calculator.strictlyDeclines(
                new int[]{10, 10, 10, 10}, new int[]{9, 7, 7, 3})).isFalse();
        assertThat(calculator.strictlyDeclines(
                new int[]{10, 10, 0, 10}, new int[]{9, 7, 0, 3})).isFalse();
    }

    @Test
    void 집계_주_수가_설정과_다르면_예외() {
        // scoredCounts.length != 4 분기
        assertThatThrownBy(() -> calculator.strictlyDeclines(
                new int[]{10, 10, 10}, new int[]{9, 7, 5}))
                .isInstanceOf(IllegalArgumentException.class);
        // correctCounts.length != 4 분기 (scored는 4, correct만 3)
        assertThatThrownBy(() -> calculator.strictlyDeclines(
                new int[]{10, 10, 10, 10}, new int[]{9, 7, 5}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
