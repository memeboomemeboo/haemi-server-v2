package com.memeboo2.haemi.elder.reminiscence;

import com.memeboo2.haemi.elder.reminiscence.domain.GeneratedReminiscence;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratedReminiscenceDomainTest {

    @Test
    void 팩토리_메서드로_AI_생성_회상콘텐츠를_만든다() {
        UUID elderId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2025, 1, 15);

        GeneratedReminiscence r = GeneratedReminiscence.of(elderId, date, "오늘의 회상 콘텐츠", true);

        assertThat(r.getId()).isNotNull();
        assertThat(r.getElderId()).isEqualTo(elderId);
        assertThat(r.getContentDate()).isEqualTo(date);
        assertThat(r.getContent()).isEqualTo("오늘의 회상 콘텐츠");
        assertThat(r.isAiGenerated()).isTrue();
    }

    @Test
    void 팩토리_메서드로_템플릿_대체_회상콘텐츠를_만든다() {
        UUID elderId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2025, 6, 1);

        GeneratedReminiscence r = GeneratedReminiscence.of(elderId, date, "템플릿 콘텐츠", false);

        assertThat(r.isAiGenerated()).isFalse();
        assertThat(r.getContent()).isEqualTo("템플릿 콘텐츠");
    }

    @Test
    void update로_콘텐츠와_AI여부를_변경한다() {
        GeneratedReminiscence r = GeneratedReminiscence.of(UUID.randomUUID(), LocalDate.now(), "원본", false);

        r.update("수정된 콘텐츠", true);

        assertThat(r.getContent()).isEqualTo("수정된 콘텐츠");
        assertThat(r.isAiGenerated()).isTrue();
    }

    @Test
    void update를_여러_번_호출할_수_있다() {
        GeneratedReminiscence r = GeneratedReminiscence.of(UUID.randomUUID(), LocalDate.now(), "v1", true);

        r.update("v2", false);
        r.update("v3", true);

        assertThat(r.getContent()).isEqualTo("v3");
        assertThat(r.isAiGenerated()).isTrue();
    }

    @Test
    void 서로_다른_어르신의_회상콘텐츠는_다른_id를_갖는다() {
        GeneratedReminiscence r1 = GeneratedReminiscence.of(UUID.randomUUID(), LocalDate.now(), "c1", true);
        GeneratedReminiscence r2 = GeneratedReminiscence.of(UUID.randomUUID(), LocalDate.now(), "c2", true);

        assertThat(r1.getId()).isNotEqualTo(r2.getId());
    }
}
