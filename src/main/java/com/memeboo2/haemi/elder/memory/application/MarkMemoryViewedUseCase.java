package com.memeboo2.haemi.elder.memory.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.event.MemoryViewed;
import com.memeboo2.haemi.common.security.ElderAccessChecked;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.elder.memory.infrastructure.MemoryViewRepository;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.api.ElderMemoryQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 어르신이 추억을 열어봤음을 기록하고 최초 열람에서만 MemoryViewed를 발행한다. (#55, B안)
 * 조회 API가 아니라 프론트의 명시적 열람 처리 엔드포인트에서 호출한다 —
 * 목록 스크롤·프리페치가 열람으로 잡히지 않도록.
 */
@Service
@RequiredArgsConstructor
public class MarkMemoryViewedUseCase {

    private final ElderMemoryQuery elderMemoryQuery;
    private final CareAccessQuery careAccessQuery;
    private final MemoryViewRepository memoryViewRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final HaemiClock clock;

    @Transactional
    @ElderAccessChecked
    public void execute(UUID elderUserId, UUID memoryId) {
        UUID elderId = careAccessQuery.elderIdForUser(elderUserId);
        careAccessQuery.requireSelf(elderUserId, elderId);

        // 본인에게 등록된 추억이 아니면 404
        if (elderMemoryQuery.findForElder(memoryId, elderId).isEmpty()) {
            throw new DomainException(ErrorCode.RESOURCE_NOT_FOUND, "추억을 찾을 수 없습니다.");
        }

        int inserted = memoryViewRepository.insertIfAbsent(elderId, memoryId, clock.now());
        if (inserted == 1) {
            eventPublisher.publishEvent(new MemoryViewed(elderId, memoryId, clock.today()));
        }
    }
}
