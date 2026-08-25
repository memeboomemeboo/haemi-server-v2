package com.memeboo2.haemi.auth.session.application;

import com.memeboo2.haemi.auth.session.infrastructure.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void purge(String token, String deviceId) {
        refreshTokenRepository.deleteByTokenAndDeviceId(token, deviceId);
    }
}
