package com.memeboo2.haemi.auth.account.domain;

import com.memeboo2.haemi.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(name = "birth_date", length = 10)
    private String birthDate;

    @Column(length = 20)
    private String phone;

    public static Account guardian(String name, String loginId, String passwordHash) {
        Account a = new Account();
        a.role = AccountRole.GUARDIAN;
        a.name = name;
        a.loginId = loginId;
        a.passwordHash = passwordHash;
        return a;
    }

    public static Account elder(String name, String loginId, String pinHash, String birthDate, String phone) {
        Account a = new Account();
        a.role = AccountRole.ELDER;
        a.name = name;
        a.loginId = loginId;
        a.passwordHash = "";
        a.pinHash = pinHash;
        a.birthDate = birthDate;
        a.phone = phone;
        return a;
    }

    public void updatePin(String pinHash) {
        this.pinHash = pinHash;
    }
}
