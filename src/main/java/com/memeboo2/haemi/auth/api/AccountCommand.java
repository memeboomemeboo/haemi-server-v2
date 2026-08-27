package com.memeboo2.haemi.auth.api;

import java.util.UUID;

public interface AccountCommand {

    /** credential: 6자리 단일 크리덴셜(#100 X2). password/pin 이중 값에서 통일됨. */
    UUID createElderAccount(String name, String loginId, String credential,
                            String birthDate, String phone, String gender);
}
