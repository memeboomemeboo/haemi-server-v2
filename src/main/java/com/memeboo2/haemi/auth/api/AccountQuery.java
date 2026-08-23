package com.memeboo2.haemi.auth.api;

import java.util.Optional;
import java.util.UUID;

public interface AccountQuery {

    record AccountInfo(UUID userId, String name, String loginId, String phone,
                       String birthDate, String profileImageUrl) {}

    Optional<AccountInfo> findById(UUID userId);

    boolean existsByLoginId(String loginId);

    void updateLoginId(UUID userId, String newLoginId);

    void updateProfileImageUrl(UUID userId, String profileImageUrl);
}
