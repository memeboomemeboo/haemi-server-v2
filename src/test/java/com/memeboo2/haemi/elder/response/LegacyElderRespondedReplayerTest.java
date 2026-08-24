package com.memeboo2.haemi.elder.response;

import com.memeboo2.haemi.elder.response.application.LegacyElderRespondedReplayer;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LegacyElderRespondedReplayerTest {

    @Test
    void 이전_이벤트_FQCN을_현재_이벤트로_변환한다() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        LegacyElderRespondedReplayer replayer = new LegacyElderRespondedReplayer(publisher);
        UUID memoryId = UUID.randomUUID();
        UUID elderId = UUID.randomUUID();

        replayer.on(new com.memeboo2.haemi.elder.api.ElderResponded(memoryId, elderId));

        verify(publisher).publishEvent(new com.memeboo2.haemi.common.event.ElderResponded(memoryId, elderId));
    }
}
