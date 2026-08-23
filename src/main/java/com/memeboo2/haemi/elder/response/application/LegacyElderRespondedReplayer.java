package com.memeboo2.haemi.elder.response.application;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** 이전 FQCN으로 저장된 event_publication을 현재 이벤트 계약으로 변환한다. */
@Component
@RequiredArgsConstructor
public class LegacyElderRespondedReplayer {

    private final ApplicationEventPublisher eventPublisher;

    @ApplicationModuleListener
    public void on(com.memeboo2.haemi.elder.api.ElderResponded event) {
        eventPublisher.publishEvent(new com.memeboo2.haemi.common.event.ElderResponded(
                event.memoryId(), event.elderId()));
    }
}
