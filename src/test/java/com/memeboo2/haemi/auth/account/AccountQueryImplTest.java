package com.memeboo2.haemi.auth.account;

import com.memeboo2.haemi.auth.account.application.AccountQueryImpl;
import com.memeboo2.haemi.auth.account.domain.Account;
import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
import com.memeboo2.haemi.auth.api.AccountQuery;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;

@ExtendWith(MockitoExtension.class)
class AccountQueryImplTest {

    @Mock AccountRepository accountRepository;
    @InjectMocks AccountQueryImpl accountQuery;

    UUID userId = UUID.randomUUID();

    private Account mockAccount(String email) {
        Account account = org.mockito.Mockito.mock(Account.class);
        org.mockito.Mockito.lenient().when(account.getId()).thenReturn(userId);
        org.mockito.Mockito.lenient().when(account.getName()).thenReturn("황정빈");
        org.mockito.Mockito.lenient().when(account.getLoginId()).thenReturn("hjbin1211");
        org.mockito.Mockito.lenient().when(account.getPhone()).thenReturn("010-1234-5678");
        org.mockito.Mockito.lenient().when(account.getBirthDate()).thenReturn("1999-01-01");
        org.mockito.Mockito.lenient().when(account.getProfileImageUrl()).thenReturn("https://image.example/profile.png");
        org.mockito.Mockito.lenient().when(account.getLastLoginAt()).thenReturn(null);
        org.mockito.Mockito.lenient().when(account.getEmail()).thenReturn(email);
        return account;
    }

    @Test
    void findById_매핑된_AccountInfo_반환() {
        Account account = mockAccount("hjbin1211@gmail.com");
        given(accountRepository.findById(userId)).willReturn(Optional.of(account));

        Optional<AccountQuery.AccountInfo> result = accountQuery.findById(userId);

        assertThat(result).isPresent();
        assertThat(result.get().userId()).isEqualTo(userId);
        assertThat(result.get().name()).isEqualTo("황정빈");
        assertThat(result.get().loginId()).isEqualTo("hjbin1211");
    }

    @Test
    void findById_없는_유저는_empty() {
        given(accountRepository.findById(userId)).willReturn(Optional.empty());

        Optional<AccountQuery.AccountInfo> result = accountQuery.findById(userId);

        assertThat(result).isEmpty();
    }

    @Test
    void findAllById_전체_매핑() {
        Account account = mockAccount("hjbin1211@gmail.com");
        given(accountRepository.findAllById(List.of(userId))).willReturn(List.of(account));

        List<AccountQuery.AccountInfo> result = accountQuery.findAllById(List.of(userId));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).userId()).isEqualTo(userId);
    }

    @Test
    void existsByLoginId_레포지토리_위임() {
        given(accountRepository.existsByLoginId("hjbin1211")).willReturn(true);

        boolean result = accountQuery.existsByLoginId("hjbin1211");

        assertThat(result).isTrue();
        then(accountRepository).should().existsByLoginId("hjbin1211");
    }

    @Test
    void updateLoginId_없는_유저는_RESOURCE_NOT_FOUND() {
        given(accountRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> accountQuery.updateLoginId(userId, "newLoginId"))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void updateLoginId_존재하는_유저는_업데이트() {
        Account account = mockAccountLenient();
        given(accountRepository.findById(userId)).willReturn(Optional.of(account));

        accountQuery.updateLoginId(userId, "newLoginId");

        then(account).should().updateLoginId("newLoginId");
    }

    @Test
    void updateProfileImageUrl_없는_유저는_예외() {
        given(accountRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> accountQuery.updateProfileImageUrl(userId, "url"))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void updateProfileImageUrl_존재하는_유저는_업데이트() {
        Account account = mockAccountLenient();
        given(accountRepository.findById(userId)).willReturn(Optional.of(account));

        accountQuery.updateProfileImageUrl(userId, "https://image.example/new.png");

        then(account).should().updateProfileImageUrl("https://image.example/new.png");
    }

    @Test
    void emailOf_이메일_반환() {
        Account account = org.mockito.Mockito.mock(Account.class);
        given(account.getEmail()).willReturn("hjbin1211@gmail.com");
        given(accountRepository.findById(userId)).willReturn(Optional.of(account));

        Optional<String> result = accountQuery.emailOf(userId);

        assertThat(result).contains("hjbin1211@gmail.com");
    }

    @Test
    void emailOf_null이면_empty() {
        Account account = org.mockito.Mockito.mock(Account.class);
        given(account.getEmail()).willReturn(null);
        given(accountRepository.findById(userId)).willReturn(Optional.of(account));

        Optional<String> result = accountQuery.emailOf(userId);

        assertThat(result).isEmpty();
    }

    @Test
    void emailOf_blank이면_empty() {
        Account account = org.mockito.Mockito.mock(Account.class);
        given(account.getEmail()).willReturn("   ");
        given(accountRepository.findById(userId)).willReturn(Optional.of(account));

        Optional<String> result = accountQuery.emailOf(userId);

        assertThat(result).isEmpty();
    }

    private Account mockAccountLenient() {
        return org.mockito.Mockito.mock(Account.class);
    }
}
