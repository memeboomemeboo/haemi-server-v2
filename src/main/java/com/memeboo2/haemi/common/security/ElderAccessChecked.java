package com.memeboo2.haemi.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 어르신 JWT 사용자 ID를 도메인 Elder ID로 해석하고 본인 접근을 검증한 유스케이스 경계. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ElderAccessChecked {
}
