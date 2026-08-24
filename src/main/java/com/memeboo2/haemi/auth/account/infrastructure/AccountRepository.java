package com.memeboo2.haemi.auth.account.infrastructure;

import com.memeboo2.haemi.auth.account.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);
}
