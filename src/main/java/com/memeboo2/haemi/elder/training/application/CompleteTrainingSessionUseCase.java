package com.memeboo2.haemi.elder.training.application;

import com.memeboo2.haemi.common.event.TrainingSessionCompleted;
import com.memeboo2.haemi.common.security.ElderAccessChecked;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 그날 인지 훈련을 마쳤다는 사실을 발행한다 — 출석·리포트 읽기 모델의 유일한 원천이다.
 *
 * <p>세션 문항·채점 상태는 이 유스케이스가 다루지 않는다 (#37 범위). 여기서는 세션 테이블을
 * 만들지 않고 "완료 사실"만 발행하므로, #37이 머지되면 이 클래스를 지우고 발행 지점을
 * TrainingSessionService로 옮기면 된다 — 소비자(elder/attendance)는 그대로 둔다.
 *
 * <p>같은 날 여러 번 호출돼도 안전하다. 소비자가 (어르신, 날짜) 기준으로 멱등 적재한다.
 */
@Service
@RequiredArgsConstructor
public class CompleteTrainingSessionUseCase {

    private final CareAccessQuery careAccessQuery;
    private final ApplicationEventPublisher eventPublisher;
    private final HaemiClock clock;

    /** @return 완료로 기록된 날짜 (KST) */
    @Transactional
    @ElderAccessChecked
    public LocalDate completeToday(UUID elderUserId) {
        UUID elderId = careAccessQuery.elderIdForUser(elderUserId);
        careAccessQuery.requireSelf(elderUserId, elderId);

        LocalDate today = clock.today();
        eventPublisher.publishEvent(new TrainingSessionCompleted(elderId, today));
        return today;
    }
}
