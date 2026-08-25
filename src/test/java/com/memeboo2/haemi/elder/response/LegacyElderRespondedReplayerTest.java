package com.memeboo2.haemi.elder.response;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.elder.response.application.LegacyElderRespondedReplayer;
import com.memeboo2.haemi.elder.response.domain.Response;
import com.memeboo2.haemi.elder.response.infrastructure.ResponseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LegacyElderRespondedReplayerTest {

    private final ZoneId KST = ZoneId.of("Asia/Seoul");

    private Response responseCreatedAt(Instant createdAt) throws Exception {
        Response r = Response.text(UUID.randomUUID(), UUID.randomUUID(), "댓글");
        Field f = Class.forName("com.memeboo2.haemi.common.persistence.BaseEntity")
                .getDeclaredField("createdAt");
        f.setAccessible(true);
        f.set(r, createdAt);
        return r;
    }

    @Test
    void 원본_응답의_생성일에서_KST_날짜를_유도한다() throws Exception {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        ResponseRepository responseRepository = mock(ResponseRepository.class);
        HaemiClock clock = mock(HaemiClock.class);
        // 실제 default 구현으로 변환하도록 위임
        given(clock.toLocalDate(any())).willAnswer(inv ->
                ((Instant) inv.getArgument(0)).atZone(KST).toLocalDate());

        LegacyElderRespondedReplayer replayer =
                new LegacyElderRespondedReplayer(publisher, responseRepository, clock);
        UUID memoryId = UUID.randomUUID();
        UUID elderId = UUID.randomUUID();

        Instant earliest = Instant.parse("2025-01-10T05:00:00Z"); // KST 2025-01-10 14:00
        Instant later = Instant.parse("2025-02-20T05:00:00Z");
        given(responseRepository.findByMemoryIdAndElderId(memoryId, elderId))
                .willReturn(List.of(responseCreatedAt(later), responseCreatedAt(earliest)));

        replayer.on(new com.memeboo2.haemi.elder.api.ElderResponded(memoryId, elderId));

        verify(publisher).publishEvent(new com.memeboo2.haemi.common.event.ElderResponded(
                memoryId, elderId, LocalDate.of(2025, 1, 10)));
    }

    @Test
    void 원본_응답이_없으면_오늘_날짜로_대체한다() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        ResponseRepository responseRepository = mock(ResponseRepository.class);
        HaemiClock clock = mock(HaemiClock.class);
        given(clock.today()).willReturn(LocalDate.of(2026, 8, 25));

        LegacyElderRespondedReplayer replayer =
                new LegacyElderRespondedReplayer(publisher, responseRepository, clock);
        UUID memoryId = UUID.randomUUID();
        UUID elderId = UUID.randomUUID();
        given(responseRepository.findByMemoryIdAndElderId(memoryId, elderId)).willReturn(List.of());

        replayer.on(new com.memeboo2.haemi.elder.api.ElderResponded(memoryId, elderId));

        verify(publisher).publishEvent(new com.memeboo2.haemi.common.event.ElderResponded(
                memoryId, elderId, LocalDate.of(2026, 8, 25)));
    }
}
