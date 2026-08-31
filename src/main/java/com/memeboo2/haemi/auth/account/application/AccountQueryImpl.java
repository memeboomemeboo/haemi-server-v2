package com.memeboo2.haemi.auth.account.application;

import com.memeboo2.haemi.auth.account.domain.Account;
import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
import com.memeboo2.haemi.auth.api.AccountQuery;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.persistence.ConstraintViolations;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountQueryImpl implements AccountQuery {

    private final AccountRepository accountRepository;
    private final MediaUploadCommand mediaUploadCommand;

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountInfo> findById(UUID userId) {
        return accountRepository.findById(userId)
                .map(a -> new AccountInfo(
                        a.getId(), a.getName(), a.getLoginId(), a.getPhone(),
                        a.getBirthDate(), mediaUploadCommand.resolveServingUrl(a.getProfileImageUrl()), a.getLastLoginAt()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> emailOf(UUID userId) {
        return accountRepository.findById(userId)
                .map(Account::getEmail)
                .filter(email -> email != null && !email.isBlank());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountInfo> findAllById(Collection<UUID> userIds) {
        return accountRepository.findAllById(userIds).stream()
                .map(a -> new AccountInfo(
                        a.getId(), a.getName(), a.getLoginId(), a.getPhone(),
                        a.getBirthDate(), mediaUploadCommand.resolveServingUrl(a.getProfileImageUrl()), a.getLastLoginAt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByLoginId(String loginId) {
        return accountRepository.existsByLoginId(loginId);
    }

    @Override
    @Transactional
    public void updateName(UUID userId, String newName) {
        Account account = accountRepository.findById(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND));
        account.updateName(newName);
    }

    @Override
    @Transactional
    public void updateBirthDate(UUID userId, String newBirthDate) {
        Account account = accountRepository.findById(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND));
        account.updateBirthDate(newBirthDate);
    }

    @Override
    @Transactional
    public void updatePhone(UUID userId, String newPhone) {
        Account account = accountRepository.findById(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND));
        account.updatePhone(newPhone);
    }

    @Override
    @Transactional
    public void updateLoginId(UUID userId, String newLoginId) {
        Account account = accountRepository.findById(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND));
        account.updateLoginId(newLoginId);
        try {
            accountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException e) {
            if (ConstraintViolations.isViolationOf(e, "uk_accounts_login_id")) {
                throw new DomainException(ErrorCode.LOGIN_ID_ALREADY_TAKEN);
            }
            throw e;
        }
    }

    @Override
    @Transactional
    public void updateProfileImageUrl(UUID userId, String profileImageUrl) {
        Account account = accountRepository.findById(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND));
        account.updateProfileImageUrl(profileImageUrl);
    }
}
