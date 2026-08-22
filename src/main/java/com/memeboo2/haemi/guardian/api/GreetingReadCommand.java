package com.memeboo2.haemi.guardian.api;

import java.util.UUID;

/**
 * 어르신이 하루 한마디를 읽음 처리하기 위한 계약.
 * 소유: guardian/dailycare. elder/inbox 가 호출.
 */
public interface GreetingReadCommand {

    void markRead(UUID elderId, UUID dailyCareId);
}
