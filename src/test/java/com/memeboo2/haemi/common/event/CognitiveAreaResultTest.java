package com.memeboo2.haemi.common.event;

import com.memeboo2.haemi.common.event.CognitiveTrainingCompleted.CognitiveAreaResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CognitiveAreaResultTest {

    @Test
    void 정상_집계는_생성된다() {
        CognitiveAreaResult r = new CognitiveAreaResult("ORIENTATION", 5, 3);
        assertThat(r.scoredAnswerCount()).isEqualTo(5);
        assertThat(r.correctAnswerCount()).isEqualTo(3);
    }

    @Test
    void 영역이_null이면_예외() {
        assertThatThrownBy(() -> new CognitiveAreaResult(null, 1, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 영역이_공백이면_예외() {
        assertThatThrownBy(() -> new CognitiveAreaResult("  ", 1, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 채점수가_음수면_예외() {
        assertThatThrownBy(() -> new CognitiveAreaResult("RECALL", -1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 정답수가_음수면_예외() {
        assertThatThrownBy(() -> new CognitiveAreaResult("RECALL", 3, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 정답수가_채점수보다_크면_예외() {
        assertThatThrownBy(() -> new CognitiveAreaResult("RECALL", 2, 3))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
