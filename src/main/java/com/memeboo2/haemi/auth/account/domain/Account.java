package com.memeboo2.haemi.auth.account.domain;

import com.memeboo2.haemi.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "accounts",
        uniqueConstraints = @UniqueConstraint(name = "uk_accounts_login_id", columnNames = "login_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountRole role;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "login_id", nullable = false, length = 50)
    private String loginId;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "pin_hash", length = 100)
    private String pinHash;

    @Column(name = "pin_login_enabled", nullable = false)
    private boolean pinLoginEnabled;

    @Column(name = "birth_date", length = 10)
    private String birthDate;

    @Column(length = 20)
    private String phone;

    /** 보호자 가입 시 인증된 이메일. 어르신 계정은 없다. */
    @Column(length = 255)
    private String email;

    @Column(length = 20)
    private String gender;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    public static Account guardian(String name, String loginId, String passwordHash,
                                   String birthDate, String phone, String email, String pinHash) {
        Account a = new Account();
        a.role = AccountRole.GUARDIAN;
        a.name = name;
        a.loginId = loginId;
        a.passwordHash = passwordHash;
        a.birthDate = birthDate;
        a.phone = phone;
        a.email = email;
        a.pinHash = pinHash;
        return a;
    }

    public static Account elder(String name, String loginId, String passwordHash, String pinHash,
                                String birthDate, String phone, String gender) {
        Account a = new Account();
        a.role = AccountRole.ELDER;
        a.name = name;
        a.loginId = loginId;
        a.passwordHash = passwordHash;
        a.pinHash = pinHash;
        a.birthDate = birthDate;
        a.phone = phone;
        a.gender = gender;
        return a;
    }

    public void updatePin(String pinHash) {
        this.pinHash = pinHash;
    }

    public void updateLoginId(String loginId) {
        this.loginId = loginId;
    }

    public void updateProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void enablePinLogin() {
        this.pinLoginEnabled = true;
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void recordLoginFailure(Instant now, int maxAttempts, long lockDurationSeconds) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= maxAttempts) {
            lockedUntil = now.plusSeconds(lockDurationSeconds);
        }
    }

}
