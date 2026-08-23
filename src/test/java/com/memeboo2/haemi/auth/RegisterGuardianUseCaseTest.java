package com.memeboo2.haemi.auth;

import com.memeboo2.haemi.auth.account.application.RegisterGuardianUseCase;
import com.memeboo2.haemi.auth.account.domain.Account;
import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
import com.memeboo2.haemi.auth.credential.PasswordService;
import com.memeboo2.haemi.auth.verification.application.PhoneVerificationUseCase;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RegisterGuardianUseCaseTest {

    @Mock AccountRepository accountRepository;
    @Mock PasswordService passwordService;
    @Mock PhoneVerificationUseCase phoneVerificationUseCase;
    @InjectMocks RegisterGuardianUseCase sut;

    @Test
    void 정상_보호자_회원가입() {
        given(accountRepository.existsByLoginId("user01")).willReturn(false);
        given(passwordService.encode("pass1234")).willReturn("hashed");
        UUID expectedId = UUID.randomUUID();
        given(accountRepository.save(any(Account.class))).willAnswer(inv -> {
            Account a = inv.getArgument(0);
            var idField = a.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(a, expectedId);
            return a;
        });

        UUID verificationId = UUID.randomUUID();
        UUID result = sut.execute("홍길동", "user01", "pass1234", "1970-01-01", "01011112222", "123456", verificationId);

        assertThat(result).isEqualTo(expectedId);
        org.mockito.Mockito.verify(phoneVerificationUseCase).consumeVerified(verificationId, "01011112222");
    }

    @Test
    void 중복_아이디_409() {
        given(accountRepository.existsByLoginId("user01")).willReturn(true);

        assertThatThrownBy(() -> sut.execute("홍길동", "user01", "pass1234", "1970-01-01", "01011112222", "123456", UUID.randomUUID()))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCode.LOGIN_ID_ALREADY_TAKEN);
    }

    @Test
    void 어르신_계정생성_정상() {
        var cmd = new com.memeboo2.haemi.auth.account.application.CreateElderAccountUseCase(
                accountRepository, passwordService);
        given(accountRepository.existsByLoginId("elder01")).willReturn(false);
        given(passwordService.encode("password1")).willReturn("password_hashed");
        given(passwordService.encode("123456")).willReturn("pin_hashed");
        UUID expectedId = UUID.randomUUID();
        given(accountRepository.save(any(Account.class))).willAnswer(inv -> {
            Account a = inv.getArgument(0);
            var idField = a.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(a, expectedId);
            return a;
        });

        UUID result = cmd.createElderAccount(
                "김할머니", "elder01", "password1", "123456", "1945-01-01", "01011112222", "FEMALE");

        assertThat(result).isEqualTo(expectedId);
    }
}
