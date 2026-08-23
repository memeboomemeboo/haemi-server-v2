package com.memeboo2.haemi.auth.api;

import java.util.UUID;

public interface AccountCommand {

    UUID createElderAccount(String name, String loginId, String password, String pin,
                            String birthDate, String phone, String gender);
}
