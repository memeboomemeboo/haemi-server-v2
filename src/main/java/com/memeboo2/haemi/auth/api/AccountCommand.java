package com.memeboo2.haemi.auth.api;

import java.util.UUID;

public interface AccountCommand {

    /** 디자인의 6자리 PIN은 필수, 별도 비밀번호는 선택으로 함께 보관한다(design-api-spec B2). */
    UUID createElderAccount(String name, String loginId, String pin, String password,
                            String birthDate, String phone, String gender);
}
