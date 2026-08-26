package com.memeboo2.haemi.elder.reminiscence.application;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 매일 08:00(Asia/Seoul) 어르신별 개인화 회상 콘텐츠 배치 생성 (PPT slide 12).
 * EC2 1대 전제로 분산 락(ShedLock)은 생략한다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "haemi.ai.reminiscence", name = "batch-enabled", havingValue = "true", matchIfMissing = true)
public class DailyReminiscenceBatch {

    private static final Logger log = LoggerFactory.getLogger(DailyReminiscenceBatch.class);

    private final CareAccessQuery careAccessQuery;
    private final ReminiscenceService reminiscenceService;
    private final HaemiClock clock;

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Seoul")
    public void run() {
        LocalDate today = clock.today();
        BatchResult result = generateForAll(today);
        log.info("개인화 회상 콘텐츠 배치 완료: date={}, 대상={}, 성공={}, 실패={}",
                today, result.total(), result.succeeded(), result.failed());
    }

    /** 전체 어르신에 대해 회상 콘텐츠를 생성한다. (스케줄러·수동 트리거 공용) */
    public BatchResult generateForAll(LocalDate date) {
        Set<UUID> elderIds = careAccessQuery.allLinks().stream()
                .map(CareAccessQuery.CareLink::elderId)
                .collect(Collectors.toSet());

        int succeeded = 0;
        int failed = 0;
        for (UUID elderId : elderIds) {
            try {
                reminiscenceService.generateForElder(elderId, date);
                succeeded++;
            } catch (RuntimeException e) {
                failed++;
                log.warn("회상 콘텐츠 생성 실패: elderId={}, date={}, cause={}", elderId, date, e.toString());
            }
        }
        return new BatchResult(elderIds.size(), succeeded, failed);
    }

    public record BatchResult(int total, int succeeded, int failed) {}
}
