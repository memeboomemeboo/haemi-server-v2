package com.memeboo2.haemi.guardian.family.application;

/** 초대 코드 unique 충돌. 바깥 트랜잭션이 새 코드로 재시도할 수 있도록 신호만 전달한다. */
public class InviteCodeConflictException extends RuntimeException {

    public InviteCodeConflictException(Throwable cause) {
        super(cause);
    }
}
