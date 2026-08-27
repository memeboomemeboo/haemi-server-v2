package com.memeboo2.haemi.auth;

import com.memeboo2.haemi.auth.account.application.CreateElderAccountUseCase;
import com.memeboo2.haemi.auth.account.application.RegisterGuardianUseCase;
import com.memeboo2.haemi.auth.account.domain.Account;
import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
import com.memeboo2.haemi.auth.credential.PasswordService;
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
    @InjectMocks RegisterGuardianUseCase sut;

    private void stubSaveAssigning(UUID id) throws Exception {
        given(accountRepository.saveAndFlush(any(Account.class))).willAnswer(inv -> {
            Account a = inv.getArgument(0);
            var idField = a.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(a, id);
            return a;
        });
    }

    @Test
    void 정상_보호자_회원가입() throws Exception {
        // #100 X1: 이메일·전화번호·이메일 인증 없이 이름·아이디·비밀번호·생년월일·PIN 만으로 가입한다.
        given(accountRepository.existsByLoginId("user01")).willReturn(false);
        given(passwordService.encode("pass1234")).willReturn("hashed");
        given(passwordService.encode("123456")).willReturn("pin_hashed");
        UUID expectedId = UUID.randomUUID();
        stubSaveAssigning(expectedId);

        UUID result = sut.execute("홍길동", "user01", "pass1234", "1970-01-01", "123456");

        assertThat(result).isEqualTo(expectedId);
    }

    @Test
    void 중복_아이디_409() {
        given(accountRepository.existsByLoginId("user01")).willReturn(true);

        assertThatThrownBy(() -> sut.execute("홍길동", "user01", "pass1234", "1970-01-01", "123456"))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCode.LOGIN_ID_ALREADY_TAKEN);
    }

    @Test
    void 동시_가입으로_로그인아이디_유니크_위반이_나면_409로_변환된다() {
        given(accountRepository.existsByLoginId("user01")).willReturn(false);
        given(accountRepository.saveAndFlush(any(Account.class))).willThrow(
                new org.springframework.dao.DataIntegrityViolationException("insert 실패",
                        new org.hibernate.exception.ConstraintViolationException(
                                "duplicate key", new java.sql.SQLException("duplicate key"), "uk_accounts_login_id")));

        assertThatThrownBy(() -> sut.execute("홍길동", "user01", "pass1234", "1970-01-01", "123456"))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCode.LOGIN_ID_ALREADY_TAKEN);
    }

    @Test
    void 어르신_계정생성_단일_6자리_크리덴셜() throws Exception {
        // #100 X2: 어르신은 6자리 단일 크리덴셜. 같은 해시가 password/pin 양쪽에 들어가고 PIN 로그인 활성화.
        var cmd = new CreateElderAccountUseCase(accountRepository, passwordService);
        given(accountRepository.existsByLoginId("elder01")).willReturn(false);
        given(passwordService.encode("123456")).willReturn("cred_hashed");
        UUID expectedId = UUID.randomUUID();
        given(accountRepository.save(any(Account.class))).willAnswer(inv -> {
            Account a = inv.getArgument(0);
            var idField = a.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(a, expectedId);
            assertThat(a.getPasswordHash()).isEqualTo("cred_hashed");
            assertThat(a.getPinHash()).isEqualTo("cred_hashed");
            assertThat(a.isPinLoginEnabled()).isTrue();
            return a;
        });

        UUID result = cmd.createElderAccount(
                "김할머니", "elder01", "123456", "1945-01-01", "01011112222", "FEMALE");

        assertThat(result).isEqualTo(expectedId);
    }
}
