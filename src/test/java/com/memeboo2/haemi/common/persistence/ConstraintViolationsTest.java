package com.memeboo2.haemi.common.persistence;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class ConstraintViolationsTest {

    @Test
    void 하이버네이트가_제약명을_파싱하면_그_이름으로_판별한다() {
        var hibernate = new ConstraintViolationException(
                "unique", new SQLException("dup"), "uk_family_elder");
        var ex = new DataIntegrityViolationException("wrap", hibernate);

        assertThat(ConstraintViolations.isViolationOf(ex, "uk_family_elder")).isTrue();
        assertThat(ConstraintViolations.isViolationOf(ex, "uk_other")).isFalse();
    }

    @Test
    void 제약명이_비어있으면_예외_메시지에서_찾는다() {
        // constraintName이 null → 메시지 폴백 분기
        // constraintName=null → 루프 탈출 후 getMostSpecificCause()(가장 깊은 SQLException) 메시지로 폴백
        // 실제 Postgres 메시지처럼 제약명이 따옴표로 감싸여 온다.
        var hibernate = new ConstraintViolationException(
                "violates constraint",
                new SQLException("duplicate key value violates unique constraint \"uk_login_id\""), null);
        var ex = new DataIntegrityViolationException("wrap", hibernate);

        assertThat(ConstraintViolations.isViolationOf(ex, "uk_login_id")).isTrue();
    }

    @Test
    void 하이버네이트_원인이_없으면_메시지로_판별한다() {
        var ex = new DataIntegrityViolationException(
                "wrap",
                new SQLException("duplicate key value violates unique constraint \"uk_device\""));

        assertThat(ConstraintViolations.isViolationOf(ex, "uk_device")).isTrue();
        assertThat(ConstraintViolations.isViolationOf(ex, "uk_absent")).isFalse();
    }

    @Test
    void 사용자입력이_다른_제약명을_포함해도_오분류하지_않는다() {
        // 이메일 값(uk_login_id를 우연히 포함)이 메시지 원문에 섞여도, 따옴표 토큰만 비교하므로 안전하다.
        var ex = new DataIntegrityViolationException(
                "wrap",
                new SQLException("Key (email)=(uk_login_id@x.com) violates unique constraint \"uk_email\""));

        assertThat(ConstraintViolations.isViolationOf(ex, "uk_email")).isTrue();
        assertThat(ConstraintViolations.isViolationOf(ex, "uk_login_id")).isFalse();
    }
}
