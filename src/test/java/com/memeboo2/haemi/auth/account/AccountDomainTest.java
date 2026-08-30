package com.memeboo2.haemi.auth.account;

import com.memeboo2.haemi.auth.account.domain.Account;
import com.memeboo2.haemi.auth.account.domain.AccountRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** Account의 보호자/어르신 팩토리와 상태 변경 메서드를 검증한다. */
class AccountDomainTest {

    @Test
    void guardian은_보호자_역할로_계정을_생성한다() {
        Account account = Account.guardian(
                "홍길동", "guardian1", "hashed-pw", "1980-01-01", "010-1234-5678", "guardian@test.com", "hashed-pin");

        assertThat(account.getRole()).isEqualTo(AccountRole.GUARDIAN);
        assertThat(account.getName()).isEqualTo("홍길동");
        assertThat(account.getLoginId()).isEqualTo("guardian1");
        assertThat(account.getPasswordHash()).isEqualTo("hashed-pw");
        assertThat(account.getBirthDate()).isEqualTo("1980-01-01");
        assertThat(account.getPhone()).isEqualTo("010-1234-5678");
        assertThat(account.getEmail()).isEqualTo("guardian@test.com");
        assertThat(account.getPinHash()).isEqualTo("hashed-pin");
        assertThat(account.isPinLoginEnabled()).isFalse();
        assertThat(account.getGender()).isNull();
    }

    @Test
    void guardian은_pinHash가_null이어도_생성할_수_있다() {
        Account account = Account.guardian(
                "홍길동", "guardian2", "hashed-pw", "1980-01-01", "010-1234-5678", "guardian2@test.com", null);

        assertThat(account.getPinHash()).isNull();
        assertThat(account.isPinLoginEnabled()).isFalse();
    }

    @Test
    void elder는_어르신_역할로_계정을_생성하고_비밀번호와_PIN에_같은_해시를_설정한다() {
        Account account = Account.elder(
                "김노인", "elder1", "credential-hash", "1945-05-05", "010-9999-8888", "M");

        assertThat(account.getRole()).isEqualTo(AccountRole.ELDER);
        assertThat(account.getName()).isEqualTo("김노인");
        assertThat(account.getLoginId()).isEqualTo("elder1");
        assertThat(account.getPasswordHash()).isEqualTo("credential-hash");
        assertThat(account.getPinHash()).isEqualTo("credential-hash");
        assertThat(account.isPinLoginEnabled()).isTrue();
        assertThat(account.getBirthDate()).isEqualTo("1945-05-05");
        assertThat(account.getPhone()).isEqualTo("010-9999-8888");
        assertThat(account.getGender()).isEqualTo("M");
        assertThat(account.getEmail()).isNull();
    }

    @Test
    void updatePin은_pinHash를_교체한다() {
        Account account = Account.guardian(
                "홍길동", "guardian3", "hashed-pw", "1980-01-01", "010-1234-5678", "guardian3@test.com", "old-pin");

        account.updatePin("new-pin");

        assertThat(account.getPinHash()).isEqualTo("new-pin");
    }

    @Test
    void updateLoginId는_로그인_아이디를_교체한다() {
        Account account = Account.elder("김노인", "elder2", "cred", "1945-05-05", "010-1111-2222", "F");

        account.updateLoginId("new-login-id");

        assertThat(account.getLoginId()).isEqualTo("new-login-id");
    }

    @Test
    void updateName과_updateBirthDate는_프로필_정보를_교체한다() {
        Account account = Account.guardian(
                "홍길동", "guardian5", "hashed-pw", "1980-01-01", "010-1234-5678", "guardian5@test.com", "pin-hash");

        account.updateName("박승아");
        account.updateBirthDate("1985-06-10");

        assertThat(account.getName()).isEqualTo("박승아");
        assertThat(account.getBirthDate()).isEqualTo("1985-06-10");
    }

    @Test
    void updateProfileImageUrl은_프로필_이미지_URL을_교체한다() {
        Account account = Account.elder("김노인", "elder3", "cred", "1945-05-05", "010-1111-2222", "F");

        account.updateProfileImageUrl("https://example.com/image.png");

        assertThat(account.getProfileImageUrl()).isEqualTo("https://example.com/image.png");
    }

    @Test
    void enablePinLogin은_PIN_로그인을_활성화한다() {
        Account account = Account.guardian(
                "홍길동", "guardian4", "hashed-pw", "1980-01-01", "010-1234-5678", "guardian4@test.com", "pin-hash");

        assertThat(account.isPinLoginEnabled()).isFalse();

        account.enablePinLogin();

        assertThat(account.isPinLoginEnabled()).isTrue();
    }

    @Test
    void isLocked는_잠금_해제_시각이_없으면_false다() {
        Account account = Account.elder("김노인", "elder4", "cred", "1945-05-05", "010-1111-2222", "F");

        assertThat(account.isLocked(Instant.now())).isFalse();
    }

    @Test
    void isLocked는_잠금_해제_시각이_현재보다_미래면_true다() {
        Account account = Account.elder("김노인", "elder5", "cred", "1945-05-05", "010-1111-2222", "F");
        Instant now = Instant.parse("2026-08-27T00:00:00Z");
        setLockedUntil(account, now.plusSeconds(60));

        assertThat(account.isLocked(now)).isTrue();
    }

    @Test
    void isLocked는_잠금_해제_시각이_현재보다_과거면_false다() {
        Account account = Account.elder("김노인", "elder6", "cred", "1945-05-05", "010-1111-2222", "F");
        Instant now = Instant.parse("2026-08-27T00:00:00Z");
        setLockedUntil(account, now.minusSeconds(60));

        assertThat(account.isLocked(now)).isFalse();
    }

    private void setLockedUntil(Account account, Instant lockedUntil) {
        try {
            var field = Account.class.getDeclaredField("lockedUntil");
            field.setAccessible(true);
            field.set(account, lockedUntil);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
