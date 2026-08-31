package com.memeboo2.haemi.common.family;

import java.util.UUID;

/** 회원가입 완료 후 보호자를 초대 코드의 가족에 합류시키는 공통 포트. */
public interface FamilyJoinCommand {

    void joinInCurrentTransaction(UUID guardianId, String inviteCode);
}
