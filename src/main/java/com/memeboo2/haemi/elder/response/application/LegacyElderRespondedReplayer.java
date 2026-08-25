package com.memeboo2.haemi.elder.response.application;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.elder.response.domain.Response;
import com.memeboo2.haemi.elder.response.infrastructure.ResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;

/** 이전 FQCN으로 저장된 event_publication을 현재 이벤트 계약으로 변환한다. */
@Component
@RequiredArgsConstructor
public class LegacyElderRespondedReplayer {

    private final ApplicationEventPublisher eventPublisher;
    private final ResponseRepository responseRepository;
    private final HaemiClock clock;

    @ApplicationModuleListener
    public void on(com.memeboo2.haemi.elder.api.ElderResponded event) {
        // 과거 응답 리플레이는 발생 날짜를 원본 응답의 생성 시각에서 유도한다.
        // clock.today()를 쓰면 과거 응답이 전부 오늘 날짜로 들어간다.
        // (memory, elder) 쌍의 첫 응답 시각 = 어르신이 그 추억에 처음 반응한 날.
        LocalDate respondedDate = responseRepository
                .findByMemoryIdAndElderId(event.memoryId(), event.elderId()).stream()
                .map(Response::getCreatedAt)
                .filter(java.util.Objects::nonNull)
                .min(Comparator.naturalOrder())
                .map(clock::toLocalDate)
                .orElseGet(clock::today);

        eventPublisher.publishEvent(new com.memeboo2.haemi.common.event.ElderResponded(
                event.memoryId(), event.elderId(), respondedDate));
    }
}
