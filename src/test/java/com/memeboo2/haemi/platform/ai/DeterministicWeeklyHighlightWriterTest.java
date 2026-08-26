package com.memeboo2.haemi.platform.ai;

import com.memeboo2.haemi.platform.ai.api.WeeklyHighlightFact;
import com.memeboo2.haemi.platform.ai.api.WeeklyHighlightPrompt;
import com.memeboo2.haemi.platform.ai.application.DeterministicWeeklyHighlightWriter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeterministicWeeklyHighlightWriterTest {

    private final DeterministicWeeklyHighlightWriter writer = new DeterministicWeeklyHighlightWriter();

    @Test
    void 잘한점을_먼저_관찰신호를_나중에_최대3줄로_생성한다() {
        var lines = writer.write(new WeeklyHighlightPrompt(
                5,
                List.of(WeeklyHighlightFact.RECALL_STRENGTH, WeeklyHighlightFact.LANGUAGE_STRENGTH),
                List.of(WeeklyHighlightFact.DELAYED_RECALL_SUPPORT, WeeklyHighlightFact.ORIENTATION_SUPPORT)
        ));

        assertThat(lines).containsExactly(
                "이번 주 5일 참여하셨어요.",
                "옛 기억을 떠올리는 시간을 잘 이어가고 계세요.",
                "앞서 본 내용을 다시 떠올리는 일은 요즘 조금 어려워하실 수 있어요."
        );
    }

    @Test
    void 점수_정답률_진단명_순위를_문구에_노출하지_않는다() {
        var lines = writer.write(new WeeklyHighlightPrompt(
                3,
                List.of(WeeklyHighlightFact.ORIENTATION_STRENGTH),
                List.of(WeeklyHighlightFact.RECALL_SUPPORT)
        ));

        assertThat(lines).allSatisfy(line -> {
            assertThat(line).doesNotContain("점수", "정답률", "%", "진단", "등", "순위");
        });
    }

    @Test
    void 참여와_인지_기록이_없어도_안전한_한줄을_반환한다() {
        var lines = writer.write(new WeeklyHighlightPrompt(0, List.of(), List.of()));

        assertThat(lines).containsExactly("다음 주에는 부담 없이 인지 훈련을 시작해 보세요.");
    }

    @Test
    void 주간_참여일은_7일을_넘길수없다() {
        assertThatThrownBy(() -> new WeeklyHighlightPrompt(8, List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 관찰신호를_잘한점으로_위장하거나_그반대로_전달할수없다() {
        assertThatThrownBy(() -> new WeeklyHighlightPrompt(
                0, List.of(WeeklyHighlightFact.RECALL_SUPPORT), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new WeeklyHighlightPrompt(
                0, List.of(), List.of(WeeklyHighlightFact.RECALL_STRENGTH)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
