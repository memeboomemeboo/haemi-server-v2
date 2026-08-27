package com.memeboo2.haemi.elder.attendance;

import com.memeboo2.haemi.elder.attendance.domain.DailyParticipation;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** DailyParticipation의 of 팩토리와 기본 플래그 상태를 검증한다. */
class DailyParticipationDomainTest {

    @Test
    void of는_전달받은_어르신과_날짜로_참여_기록을_생성한다() {
        UUID elderId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 8, 27);

        DailyParticipation participation = DailyParticipation.of(elderId, date);

        assertThat(participation.getElderId()).isEqualTo(elderId);
        assertThat(participation.getParticipationDate()).isEqualTo(date);
    }

    @Test
    void of로_생성한_직후에는_모든_완료_플래그가_false다() {
        DailyParticipation participation = DailyParticipation.of(UUID.randomUUID(), LocalDate.of(2026, 8, 27));

        assertThat(participation.isTrainingDone()).isFalse();
        assertThat(participation.isGreetingReadDone()).isFalse();
        assertThat(participation.isMemoryViewedDone()).isFalse();
        assertThat(participation.isRepliedDone()).isFalse();
    }

    @Test
    void 같은_어르신이어도_날짜가_다르면_별개의_참여_기록이_생성된다() {
        UUID elderId = UUID.randomUUID();
        LocalDate day1 = LocalDate.of(2026, 8, 26);
        LocalDate day2 = LocalDate.of(2026, 8, 27);

        DailyParticipation p1 = DailyParticipation.of(elderId, day1);
        DailyParticipation p2 = DailyParticipation.of(elderId, day2);

        assertThat(p1.getParticipationDate()).isNotEqualTo(p2.getParticipationDate());
        assertThat(p1.getElderId()).isEqualTo(p2.getElderId());
    }
}
