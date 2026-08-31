package com.memeboo2.haemi.auth.account.infrastructure;

import com.memeboo2.haemi.auth.account.domain.Account;
import com.memeboo2.haemi.auth.account.domain.AccountRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    /**
     * 어르신 전용 PIN 로그인은 loginId를 받지 않으므로 어르신 계정의 PIN 해시만 검증한다.
     * PIN은 BCrypt 해시로 저장되어 DB 동등 비교를 할 수 없다.
     */
    List<Account> findAllByRole(AccountRole role);

    /**
     * 로그인 실패 카운터를 DB에서 직접 증가시키고, 임계값에 도달하면 같은 UPDATE에서 잠금까지 건다.
     * 엔티티를 읽어 +1 하는 방식은 동시 실패 요청이 같은 값을 읽고 서로를 덮어써
     * 실제로는 카운트가 누락되고 계정 잠금이 우회된다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Account a
               SET a.failedLoginAttempts = CASE WHEN a.lockedUntil IS NOT NULL AND a.lockedUntil <= :now
                                                THEN 1
                                                ELSE a.failedLoginAttempts + 1 END,
                   a.lockedUntil = CASE
                                        WHEN (CASE WHEN a.lockedUntil IS NOT NULL AND a.lockedUntil <= :now
                                                   THEN 1
                                                   ELSE a.failedLoginAttempts + 1 END) >= :maxAttempts
                                             THEN :lockedUntil
                                        WHEN a.lockedUntil IS NOT NULL AND a.lockedUntil <= :now
                                             THEN NULL
                                        ELSE a.lockedUntil END
             WHERE a.loginId = :loginId
            """)
    int incrementLoginFailure(@Param("loginId") String loginId,
                              @Param("maxAttempts") int maxAttempts,
                              @Param("lockedUntil") Instant lockedUntil,
                              @Param("now") Instant now);

    /**
     * 로그인 성공 상태를 원자적으로 기록한다. 잠긴 계정에는 적용되지 않는다(0 반환).
     * 엔티티를 수정해 flush하면, 조회 시점 이후 다른 요청이 올린 실패 카운터와 잠금을
     * stale 값으로 덮어써 잠금이 풀린다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Account a
               SET a.failedLoginAttempts = 0,
                   a.lockedUntil = NULL,
                   a.lastLoginAt = :now
             WHERE a.id = :id
               AND (a.lockedUntil IS NULL OR a.lockedUntil <= :now)
            """)
    int recordLoginSuccess(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Account a SET a.pinLoginEnabled = true WHERE a.id = :id")
    int enablePinLogin(@Param("id") UUID id);
}
