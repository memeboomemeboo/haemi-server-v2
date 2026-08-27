package com.memeboo2.haemi.guardian.report;

import com.memeboo2.haemi.guardian.report.domain.ReportParticipation;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** ReportParticipation의 of 팩토리와 기본 플래그 상태를 검증한다. */
class ReportParticipationDomainTest {

    @Test
    void of는_전달받은_어르신과_날짜로_참여_기록을_생성한다() {
        UUID elderId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 8, 27);

        ReportParticipation participation = ReportParticipation.of(elderId, date);

        assertThat(participation.getElderId()).isEqualTo(elderId);
        assertThat(participation.getParticipationDate()).isEqualTo(date);
    }

    @Test
    void of로_생성한_직후에는_모든_완료_플래그가_false다() {
        ReportParticipation participation = ReportParticipation.of(UUID.randomUUID(), LocalDate.of(2026, 8, 27));

        assertThat(participation.isTrainingDone()).isFalse();
        assertThat(participation.isGreetingReadDone()).isFalse();
        assertThat(participation.isMemoryViewedDone()).isFalse();
        assertThat(participation.isRepliedDone()).isFalse();
    }

    @Test
    void 서로_다른_어르신은_각각_독립된_참여_기록을_가진다() {
        LocalDate date = LocalDate.of(2026, 8, 27);
        UUID elder1 = UUID.randomUUID();
        UUID elder2 = UUID.randomUUID();

        ReportParticipation p1 = ReportParticipation.of(elder1, date);
        ReportParticipation p2 = ReportParticipation.of(elder2, date);

        assertThat(p1.getElderId()).isNotEqualTo(p2.getElderId());
        assertThat(p1.getParticipationDate()).isEqualTo(p2.getParticipationDate());
    }
}
