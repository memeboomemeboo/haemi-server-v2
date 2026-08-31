package com.memeboo2.haemi.elder.reminiscence;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.ElderProfileQuery;
import com.memeboo2.haemi.guardian.api.ElderQuery;
import com.memeboo2.haemi.elder.reminiscence.application.AiTextGenerator;
import com.memeboo2.haemi.elder.reminiscence.application.GeneratedReminiscenceSaver;
import com.memeboo2.haemi.elder.reminiscence.application.ReminiscenceService;
import com.memeboo2.haemi.elder.reminiscence.domain.GeneratedReminiscence;
import com.memeboo2.haemi.elder.reminiscence.infrastructure.GeneratedReminiscenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReminiscenceServiceTest {

    @Mock AiTextGenerator generator;
    @Mock ElderQuery elderQuery;
    @Mock ElderProfileQuery elderProfileQuery;
    @Mock GeneratedReminiscenceRepository repository;
    @Mock GeneratedReminiscenceSaver saver;
    @Mock HaemiClock clock;

    ReminiscenceService service() {
        return new ReminiscenceService(generator, elderQuery, elderProfileQuery, repository, saver, clock);
    }

    @Test
    void 생성_문구와_live를_saver에_넘겨_저장한다() {
        UUID elderId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 8, 26);
        given(elderQuery.findById(elderId)).willReturn(Optional.of(
                new ElderQuery.ElderInfo(elderId, "김순자", Instant.now())));
        lenient().when(elderProfileQuery.findById(elderId))
                .thenReturn(new ElderProfileQuery.ElderProfile(LocalDate.of(1948, 1, 1), Instant.now()));
        given(generator.generate(any())).willReturn(new AiTextGenerator.Result("오늘의 회상 문구", true));
        GeneratedReminiscence saved = GeneratedReminiscence.of(elderId, date, "오늘의 회상 문구", true);
        given(saver.upsert(elderId, date, "오늘의 회상 문구", true)).willReturn(saved);

        GeneratedReminiscence result = service().generateForElder(elderId, date);

        assertThat(result).isSameAs(saved);
        verify(saver).upsert(elderId, date, "오늘의 회상 문구", true);
    }

    @Test
    void 응답이_2000자를_넘으면_잘라서_saver에_넘긴다() {
        UUID elderId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 8, 26);
        given(elderQuery.findById(elderId)).willReturn(Optional.of(
                new ElderQuery.ElderInfo(elderId, "김순자", Instant.now())));
        lenient().when(elderProfileQuery.findById(elderId))
                .thenReturn(new ElderProfileQuery.ElderProfile(null, Instant.now()));
        given(generator.generate(any())).willReturn(new AiTextGenerator.Result("가".repeat(3000), true));
        given(saver.upsert(any(), any(), any(), eq(true))).willAnswer(inv ->
                GeneratedReminiscence.of(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2), inv.getArgument(3)));

        service().generateForElder(elderId, date);

        ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
        verify(saver).upsert(eq(elderId), eq(date), content.capture(), eq(true));
        assertThat(content.getValue()).hasSize(2000);
    }
}
