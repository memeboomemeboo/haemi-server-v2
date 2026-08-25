package com.memeboo2.haemi.auth.session.infrastructure;

import com.memeboo2.haemi.auth.session.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByTokenAndDeviceId(String token, String deviceId);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.accountId = :accountId AND rt.deviceId = :deviceId")
    void deleteByAccountIdAndDeviceId(UUID accountId, String deviceId);

    /** 특정 토큰 행을 조건부로 제거한다. 반환값 1 = 이 요청이 토큰을 소비함(회전 진행), 0 = 경쟁 요청이 이미 소비함. */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.token = :token AND rt.deviceId = :deviceId")
    int deleteByTokenAndDeviceId(String token, String deviceId);

    /** 만료 시각이 지난 refresh 토큰 행을 일괄 삭제한다. 반환값 = 삭제된 행 수. */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    int deleteExpired(Instant now);
}
