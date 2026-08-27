package com.memeboo2.haemi.guardian.report;

import com.memeboo2.haemi.guardian.report.domain.CognitiveResultSnapshot;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** CognitiveResultSnapshot의 of 팩토리를 검증한다. */
class CognitiveResultSnapshotDomainTest {

    @Test
    void of는_전달받은_값으로_스냅샷을_생성한다() {
        UUID elderId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        LocalDate sessionDate = LocalDate.of(2026, 8, 27);

        CognitiveResultSnapshot snapshot = CognitiveResultSnapshot.of(
                elderId, sessionId, sessionDate, "ORIENTATION", 5, 4);

        assertThat(snapshot.getElderId()).isEqualTo(elderId);
        assertThat(snapshot.getSessionId()).isEqualTo(sessionId);
        assertThat(snapshot.getSessionDate()).isEqualTo(sessionDate);
        assertThat(snapshot.getCognitiveArea()).isEqualTo("ORIENTATION");
        assertThat(snapshot.getScoredAnswerCount()).isEqualTo(5);
        assertThat(snapshot.getCorrectAnswerCount()).isEqualTo(4);
    }

    @Test
    void 정답_수가_0이어도_생성할_수_있다() {
        CognitiveResultSnapshot snapshot = CognitiveResultSnapshot.of(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 8, 27), "RECALL", 3, 0);

        assertThat(snapshot.getCorrectAnswerCount()).isEqualTo(0);
        assertThat(snapshot.getScoredAnswerCount()).isEqualTo(3);
    }

    @Test
    void 서로_다른_인지_영역으로_각각_생성할_수_있다() {
        UUID sessionId = UUID.randomUUID();
        UUID elderId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 8, 27);

        CognitiveResultSnapshot orientation = CognitiveResultSnapshot.of(
                elderId, sessionId, date, "ORIENTATION", 5, 5);
        CognitiveResultSnapshot language = CognitiveResultSnapshot.of(
                elderId, sessionId, date, "LANGUAGE", 5, 3);

        assertThat(orientation.getCognitiveArea()).isEqualTo("ORIENTATION");
        assertThat(language.getCognitiveArea()).isEqualTo("LANGUAGE");
        assertThat(orientation.getSessionId()).isEqualTo(language.getSessionId());
    }
}
