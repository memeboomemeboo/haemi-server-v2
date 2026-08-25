package com.memeboo2.haemi.auth.verification.infrastructure;

import com.memeboo2.haemi.auth.verification.domain.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {

    /**
     * 실패 카운터를 DB에서 직접 증가시킨다.
     * 엔티티를 읽어 +1 하는 방식은 동시 오답 요청이 같은 값을 읽고 서로를 덮어써
     * 5회 제한이 실제로는 누적되지 않는다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE EmailVerification v SET v.failCount = v.failCount + 1 WHERE v.id = :id")
    int incrementFailCount(@Param("id") UUID id);

    @Query("SELECT v.failCount FROM EmailVerification v WHERE v.id = :id")
    Integer findFailCount(@Param("id") UUID id);
}
