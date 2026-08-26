package com.memeboo2.haemi.guardian.report.application;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 정기 리포트 발송 스케줄러 (Asia/Seoul).
 * <ul>
 *   <li>주간: 매주 월요일 08:00</li>
 *   <li>월간: 매월 1일 08:00</li>
 * </ul>
 * EC2 1대 전제로 분산 락(ShedLock)은 생략한다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "haemi.report.pdf", name = "delivery-enabled", havingValue = "true", matchIfMissing = true)
public class ReportDeliveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReportDeliveryScheduler.class);

    private final ReportDeliveryService deliveryService;

    @Scheduled(cron = "0 0 8 * * MON", zone = "Asia/Seoul")
    public void sendWeekly() {
        log.info("주간 리포트 정기 발송 시작");
        deliveryService.dispatchAll(ReportPeriod.WEEKLY);
    }

    @Scheduled(cron = "0 0 8 1 * *", zone = "Asia/Seoul")
    public void sendMonthly() {
        log.info("월간 리포트 정기 발송 시작");
        deliveryService.dispatchAll(ReportPeriod.MONTHLY);
    }
}
