package com.memeboo2.haemi.auth.session.application;

import com.memeboo2.haemi.auth.session.infrastructure.RefreshTokenRepository;
import com.memeboo2.haemi.common.time.HaemiClock;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 만료 refresh 토큰 정리를 호출 트랜잭션과 분리한다.
 * 재발급 유스케이스는 만료를 감지하면 예외로 롤백하므로, 같은 트랜잭션에서 삭제하면
 * 정리가 함께 롤백돼 만료 행이 남는다. REQUIRES_NEW로 독립 커밋해 실제로 지운다.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenMaintenance {

    private final RefreshTokenRepository refreshTokenRepository;
    private final HaemiClock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void purge(String token, String deviceId) {
        refreshTokenRepository.deleteByTokenAndDeviceId(token, deviceId);
    }

    /**
     * 만료 시각이 지난 refresh 토큰 행을 주기적으로 정리한다.
     *
     * <p>일반 만료 경로에서는 JWT가 만료되면 재발급 유스케이스의 첫 서명 검증에서 즉시 거부돼
     * 저장소 만료 분기에 도달하지 못한다. 그 결과 만료 행이 계속 누적되므로,
     * 만료 시각 기준으로 매시 정각에 일괄 삭제해 잔존 행을 정리한다.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void purgeExpired() {
        refreshTokenRepository.deleteExpired(clock.now());
    }
}
