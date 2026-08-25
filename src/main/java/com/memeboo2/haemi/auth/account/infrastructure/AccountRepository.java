package com.memeboo2.haemi.auth.account.infrastructure;

import com.memeboo2.haemi.auth.account.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    /**
     * 로그인 실패 카운터를 DB에서 직접 증가시키고, 임계값에 도달하면 같은 UPDATE에서 잠금까지 건다.
     * 엔티티를 읽어 +1 하는 방식은 동시 실패 요청이 같은 값을 읽고 서로를 덮어써
     * 실제로는 카운트가 누락되고 계정 잠금이 우회된다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Account a
               SET a.failedLoginAttempts = a.failedLoginAttempts + 1,
                   a.lockedUntil = CASE WHEN a.failedLoginAttempts + 1 >= :maxAttempts
                                        THEN :lockedUntil ELSE a.lockedUntil END
             WHERE a.loginId = :loginId
            """)
    int incrementLoginFailure(@Param("loginId") String loginId,
                              @Param("maxAttempts") int maxAttempts,
                              @Param("lockedUntil") Instant lockedUntil);
}
