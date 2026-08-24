package com.memeboo2.haemi.auth.account.application;

import com.memeboo2.haemi.auth.account.domain.Account;
import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
import com.memeboo2.haemi.auth.api.AccountQuery;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountQueryImpl implements AccountQuery {

    private final AccountRepository accountRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountInfo> findById(UUID userId) {
        return accountRepository.findById(userId)
                .map(a -> new AccountInfo(
                        a.getId(), a.getName(), a.getLoginId(), a.getPhone(),
                        a.getBirthDate(), a.getProfileImageUrl(), a.getLastLoginAt()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByLoginId(String loginId) {
        return accountRepository.existsByLoginId(loginId);
    }

    @Override
    @Transactional
    public void updateLoginId(UUID userId, String newLoginId) {
        Account account = accountRepository.findById(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND));
        account.updateLoginId(newLoginId);
    }

    @Override
    @Transactional
    public void updateProfileImageUrl(UUID userId, String profileImageUrl) {
        Account account = accountRepository.findById(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND));
        account.updateProfileImageUrl(profileImageUrl);
    }
}
