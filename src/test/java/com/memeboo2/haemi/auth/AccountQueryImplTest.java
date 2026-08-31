package com.memeboo2.haemi.auth;

import com.memeboo2.haemi.auth.account.application.AccountQueryImpl;
import com.memeboo2.haemi.auth.account.domain.Account;
import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountQueryImplTest {

    @Test
    void 로그인_ID_변경의_유니크_경합은_409으로_변환한다() {
        AccountRepository repository = mock(AccountRepository.class);
        Account account = Account.guardian("보호자", "before", "password", "1970-01-01", "01000000000", null, "pin");
        UUID accountId = UUID.randomUUID();
        when(repository.findById(accountId)).thenReturn(Optional.of(account));
        when(repository.saveAndFlush(any())).thenThrow(new org.springframework.dao.DataIntegrityViolationException(
                "duplicate", new ConstraintViolationException("duplicate", new SQLException("duplicate"),
                "uk_accounts_login_id")));
        AccountQueryImpl useCase = new AccountQueryImpl(repository, mock(MediaUploadCommand.class));

        assertThatThrownBy(() -> useCase.updateLoginId(accountId, "taken"))
                .isInstanceOf(DomainException.class)
                .extracting(error -> ((DomainException) error).getErrorCode())
                .isEqualTo(ErrorCode.LOGIN_ID_ALREADY_TAKEN);
    }
}
