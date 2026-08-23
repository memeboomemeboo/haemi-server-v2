package com.memeboo2.haemi.auth.session.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "account_id", nullable = false, columnDefinition = "uuid")
    private UUID accountId;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(nullable = false, length = 512)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void prePersist() {
        if (id == null) id = UUID.randomUUID();
    }

    public static RefreshToken of(UUID accountId, String deviceId, String token, Instant expiresAt) {
        RefreshToken rt = new RefreshToken();
        rt.accountId = accountId;
        rt.deviceId = deviceId;
        rt.token = token;
        rt.expiresAt = expiresAt;
        rt.createdAt = Instant.now();
        return rt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
