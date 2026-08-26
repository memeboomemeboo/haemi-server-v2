package com.memeboo2.haemi.elder.reminiscence;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.ElderProfileQuery;
import com.memeboo2.haemi.guardian.api.ElderQuery;
import com.memeboo2.haemi.elder.reminiscence.application.AiTextGenerator;
import com.memeboo2.haemi.elder.reminiscence.application.ReminiscenceService;
import com.memeboo2.haemi.elder.reminiscence.domain.GeneratedReminiscence;
import com.memeboo2.haemi.elder.reminiscence.infrastructure.GeneratedReminiscenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReminiscenceServiceTest {

    @Mock AiTextGenerator generator;
    @Mock ElderQuery elderQuery;
    @Mock ElderProfileQuery elderProfileQuery;
    @Mock GeneratedReminiscenceRepository repository;
    @Mock HaemiClock clock;

    ReminiscenceService service() {
        return new ReminiscenceService(generator, elderQuery, elderProfileQuery, repository, clock);
    }

    @Test
    void 신규_어르신은_생성_저장된다() {
        UUID elderId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 8, 26);
        given(elderQuery.findById(elderId)).willReturn(Optional.of(
                new ElderQuery.ElderInfo(elderId, "김순자", Instant.now())));
        lenient().when(elderProfileQuery.findById(elderId))
                .thenReturn(new ElderProfileQuery.ElderProfile(LocalDate.of(1948, 1, 1), Instant.now()));
        given(generator.generate(any())).willReturn("오늘의 회상 문구");
        given(generator.isLive()).willReturn(true);
        given(repository.findByElderIdAndContentDate(elderId, date)).willReturn(Optional.empty());
        given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

        GeneratedReminiscence result = service().generateForElder(elderId, date);

        assertThat(result.getContent()).isEqualTo("오늘의 회상 문구");
        assertThat(result.isAiGenerated()).isTrue();
        assertThat(result.getElderId()).isEqualTo(elderId);
        verify(repository).save(any());
    }

    @Test
    void 응답이_2000자를_넘으면_잘라서_저장한다() {
        UUID elderId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 8, 26);
        given(elderQuery.findById(elderId)).willReturn(Optional.of(
                new ElderQuery.ElderInfo(elderId, "김순자", Instant.now())));
        lenient().when(elderProfileQuery.findById(elderId))
                .thenReturn(new ElderProfileQuery.ElderProfile(null, Instant.now()));
        given(generator.generate(any())).willReturn("가".repeat(3000));
        given(generator.isLive()).willReturn(true);
        given(repository.findByElderIdAndContentDate(elderId, date)).willReturn(Optional.empty());
        given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

        GeneratedReminiscence result = service().generateForElder(elderId, date);

        assertThat(result.getContent()).hasSize(2000);
    }

    @Test
    void 기존_콘텐츠는_갱신된다() {
        UUID elderId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 8, 26);
        GeneratedReminiscence existing = GeneratedReminiscence.of(elderId, date, "이전 문구", false);
        given(elderQuery.findById(elderId)).willReturn(Optional.of(
                new ElderQuery.ElderInfo(elderId, "박영수", Instant.now())));
        lenient().when(elderProfileQuery.findById(elderId))
                .thenReturn(new ElderProfileQuery.ElderProfile(null, Instant.now()));
        given(generator.generate(any())).willReturn("새 문구");
        given(generator.isLive()).willReturn(false);
        given(repository.findByElderIdAndContentDate(elderId, date)).willReturn(Optional.of(existing));

        GeneratedReminiscence result = service().generateForElder(elderId, date);

        assertThat(result).isSameAs(existing);
        assertThat(result.getContent()).isEqualTo("새 문구");
        verify(repository, org.mockito.Mockito.never()).save(any());
    }
}
