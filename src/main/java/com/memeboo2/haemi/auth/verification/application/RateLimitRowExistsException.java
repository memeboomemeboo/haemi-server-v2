package com.memeboo2.haemi.auth.verification.application;

/** 같은 윈도우 카운터 행이 이미 있음. 호출자가 증가로 전환할 수 있도록 신호만 전달한다. */
public class RateLimitRowExistsException extends RuntimeException {

    public RateLimitRowExistsException(Throwable cause) {
        super(cause);
    }
}
