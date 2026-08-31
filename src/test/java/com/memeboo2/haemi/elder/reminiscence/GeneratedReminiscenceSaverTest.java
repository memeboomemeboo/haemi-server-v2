package com.memeboo2.haemi.elder.reminiscence;

import com.memeboo2.haemi.elder.reminiscence.application.GeneratedReminiscenceSaver;
import com.memeboo2.haemi.elder.reminiscence.domain.GeneratedReminiscence;
import com.memeboo2.haemi.elder.reminiscence.infrastructure.GeneratedReminiscenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GeneratedReminiscenceSaverTest {

    @Mock GeneratedReminiscenceRepository repository;

    GeneratedReminiscenceSaver saver() {
        return new GeneratedReminiscenceSaver(repository);
    }

    @Test
    void 신규는_생성_저장된다() {
        UUID elderId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 8, 26);
        given(repository.findByElderIdAndContentDate(elderId, date)).willReturn(Optional.empty());
        given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

        GeneratedReminiscence result = saver().upsert(elderId, date, "오늘의 회상 문구", true);

        assertThat(result.getContent()).isEqualTo("오늘의 회상 문구");
        assertThat(result.isAiGenerated()).isTrue();
        assertThat(result.getElderId()).isEqualTo(elderId);
        verify(repository).save(any());
    }

    @Test
    void 기존_콘텐츠는_갱신되고_새로_저장하지_않는다() {
        UUID elderId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 8, 26);
        GeneratedReminiscence existing = GeneratedReminiscence.of(elderId, date, "이전 문구", false);
        given(repository.findByElderIdAndContentDate(elderId, date)).willReturn(Optional.of(existing));

        GeneratedReminiscence result = saver().upsert(elderId, date, "새 문구", true);

        assertThat(result).isSameAs(existing);
        assertThat(result.getContent()).isEqualTo("새 문구");
        assertThat(result.isAiGenerated()).isTrue();
        verify(repository, never()).save(any());
    }
}
