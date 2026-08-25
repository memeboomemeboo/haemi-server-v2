package com.memeboo2.haemi.guardian.dailycare.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.event.GreetingRead;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.GreetingReadCommand;
import com.memeboo2.haemi.guardian.dailycare.domain.DailyCare;
import com.memeboo2.haemi.guardian.dailycare.infrastructure.DailyCareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GreetingReadCommandImpl implements GreetingReadCommand {

    private final DailyCareRepository dailyCareRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final HaemiClock clock;

    @Override
    @Transactional
    public void markRead(UUID elderId, UUID dailyCareId) {
        // #64-2: 존재하지 않으면 404, 다른 어르신의 항목이면 403 (기존엔 조용히 200)
        DailyCare care = dailyCareRepository.findById(dailyCareId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!care.getElderId().equals(elderId)) {
            throw new DomainException(ErrorCode.CARE_ACCESS_DENIED);
        }

        // 최초 열람에서만 이벤트를 발행한다 (markViewed는 최초 1회만 상태를 바꾼다).
        // 읽은 시각과 이벤트 날짜를 같은 Instant에서 파생해 KST 자정 경계 불일치를 막는다.
        Instant readAt = clock.now();
        boolean firstRead = !care.isRead();
        care.markViewed(readAt);
        if (firstRead) {
            eventPublisher.publishEvent(new GreetingRead(elderId, dailyCareId, clock.toLocalDate(readAt)));
        }
    }
}
