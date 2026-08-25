package com.memeboo2.haemi.auth.api;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountQuery {

    record AccountInfo(UUID userId, String name, String loginId, String phone,
                       String birthDate, String profileImageUrl, Instant lastLoginAt) {}

    Optional<AccountInfo> findById(UUID userId);

    /** 목록 조회용 일괄 조회 — N+1 방지. */
    List<AccountInfo> findAllById(Collection<UUID> userIds);

    boolean existsByLoginId(String loginId);

    void updateLoginId(UUID userId, String newLoginId);

    void updateProfileImageUrl(UUID userId, String profileImageUrl);
}
