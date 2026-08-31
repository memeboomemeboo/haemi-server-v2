package com.memeboo2.haemi.auth;

import com.memeboo2.haemi.auth.account.application.CreateElderAccountUseCase;
import com.memeboo2.haemi.auth.account.application.RegisterGuardianUseCase;
import com.memeboo2.haemi.auth.account.domain.Account;
import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
import com.memeboo2.haemi.auth.credential.PasswordService;
import com.memeboo2.haemi.auth.verification.application.EmailVerificationUseCase;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.family.FamilyJoinCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class RegisterGuardianUseCaseTest {

    @Mock AccountRepository accountRepository;
    @Mock PasswordService passwordService;
    @Mock EmailVerificationUseCase emailVerificationUseCase;
    @Mock FamilyJoinCommand familyJoinCommand;
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
    void 초대코드가_입력되면_가입한_보호자를_같은_트랜잭션에서_가족에_합류시킨다() throws Exception {
        UUID expectedId = UUID.randomUUID();
        given(accountRepository.existsByLoginId("user01")).willReturn(false);
        given(passwordService.encode("pass1234")).willReturn("hashed");
        given(passwordService.encode("123456")).willReturn("pin_hashed");
        stubSaveAssigning(expectedId);

        UUID result = sut.execute("홍길동", "user01", "pass1234", "1970-01-01", "123456",
                null, null, null, " INVITE123 ");

        assertThat(result).isEqualTo(expectedId);
        then(familyJoinCommand).should().joinInCurrentTransaction(expectedId, "INVITE123");
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
    void 선택_이메일에_인증ID를_제공하면_인증을_한번_소비한다() throws Exception {
        UUID verificationId = UUID.randomUUID();
        UUID expectedId = UUID.randomUUID();
        given(accountRepository.existsByLoginId("user01")).willReturn(false);
        given(passwordService.encode("pass1234")).willReturn("hashed");
        given(passwordService.encode("123456")).willReturn("pin_hashed");
        stubSaveAssigning(expectedId);

        UUID result = sut.execute("홍길동", "user01", "pass1234", "1970-01-01", "123456",
                "01012345678", "user@example.com", verificationId);

        assertThat(result).isEqualTo(expectedId);
        then(emailVerificationUseCase).should().consumeVerified(verificationId, "user@example.com");
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
    void 동시_가입으로_이메일_유니크_위반이_나면_409로_변환된다() {
        given(accountRepository.existsByLoginId("user01")).willReturn(false);
        given(accountRepository.saveAndFlush(any(Account.class))).willThrow(
                new org.springframework.dao.DataIntegrityViolationException("insert 실패",
                        new org.hibernate.exception.ConstraintViolationException(
                                "duplicate key", new java.sql.SQLException("duplicate key"), "uk_accounts_email")));

        assertThatThrownBy(() -> sut.execute("홍길동", "user01", "pass1234", "1970-01-01", "123456",
                "01012345678", "user@example.com", null))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_ALREADY_TAKEN);
    }

    @Test
    void 어르신_계정생성_단일_6자리_크리덴셜() throws Exception {
        // #100 X2: 어르신은 6자리 단일 크리덴셜. 같은 해시가 password/pin 양쪽에 들어가고 PIN 로그인 활성화.
        var cmd = new CreateElderAccountUseCase(accountRepository, passwordService);
        given(accountRepository.existsByLoginId("elder01")).willReturn(false);
        given(passwordService.encode("123456")).willReturn("cred_hashed");
        UUID expectedId = UUID.randomUUID();
        given(accountRepository.saveAndFlush(any(Account.class))).willAnswer(inv -> {
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

    @Test
    void 어르신_PIN이_이미_사용중이면_등록을_거부한다() {
        var cmd = new CreateElderAccountUseCase(accountRepository, passwordService);
        Account existingElder = Account.elder("기존 어르신", "elder01", "password-hash", "pin-hash",
                "1945-01-01", null, null);
        given(accountRepository.existsByLoginId("elder02")).willReturn(false);
        given(accountRepository.findAllByRole(com.memeboo2.haemi.auth.account.domain.AccountRole.ELDER))
                .willReturn(List.of(existingElder));
        given(passwordService.matches("123456", "pin-hash")).willReturn(true);

        assertThatThrownBy(() -> cmd.createElderAccount("새 어르신", "elder02", "123456",
                "1945-01-01", null, null))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCode.PIN_ALREADY_TAKEN);
    }

    @Test
    void 어르신_계정은_선택_비밀번호가_있으면_PIN과_별도로_저장한다() throws Exception {
        var cmd = new CreateElderAccountUseCase(accountRepository, passwordService);
        given(accountRepository.existsByLoginId("elder02")).willReturn(false);
        given(passwordService.encode("123456")).willReturn("pin_hashed");
        given(passwordService.encode("password123")).willReturn("password_hashed");
        given(accountRepository.saveAndFlush(any(Account.class))).willAnswer(invocation -> invocation.getArgument(0));

        cmd.createElderAccount("김할머니", "elder02", "123456", "password123",
                "1945-01-01", "01011112222", "FEMALE");

        org.mockito.ArgumentCaptor<Account> captor = org.mockito.ArgumentCaptor.forClass(Account.class);
        org.mockito.Mockito.verify(accountRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getPinHash()).isEqualTo("pin_hashed");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("password_hashed");
    }

    @Test
    void 어르신_등록의_동시_아이디_유니크위반은_409로_변환한다() {
        var cmd = new CreateElderAccountUseCase(accountRepository, passwordService);
        given(accountRepository.existsByLoginId("elder03")).willReturn(false);
        given(passwordService.encode("123456")).willReturn("pin_hashed");
        given(accountRepository.saveAndFlush(any(Account.class))).willThrow(
                new org.springframework.dao.DataIntegrityViolationException("insert 실패",
                        new org.hibernate.exception.ConstraintViolationException(
                                "duplicate key", new java.sql.SQLException("duplicate key"), "uk_accounts_login_id")));

        assertThatThrownBy(() -> cmd.createElderAccount("김할머니", "elder03", "123456", null,
                "1945-01-01", "01011112222", "FEMALE"))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCode.LOGIN_ID_ALREADY_TAKEN);
    }
}
