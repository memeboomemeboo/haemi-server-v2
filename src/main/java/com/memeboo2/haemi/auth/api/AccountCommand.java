package com.memeboo2.haemi.auth.api;

import java.util.UUID;

public interface AccountCommand {

    UUID createElderAccount(String name, String loginId, String pin, String birthDate, String phone);
}
