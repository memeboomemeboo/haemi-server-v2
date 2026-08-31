package com.memeboo2.haemi.elder.inbox.application;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.common.security.ElderAccessChecked;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.api.GreetingQuery;
import com.memeboo2.haemi.guardian.api.GreetingQuery.ReceivedGreeting;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** 어르신이 오늘 받은 하루 한마디 조회 (R6: 본인 수신분만). */
@Service
@RequiredArgsConstructor
public class GetInboxUseCase {

    private final GreetingQuery greetingQuery;
    private final CareAccessQuery careAccessQuery;
    private final HaemiClock clock;

    @ElderAccessChecked
    public List<ReceivedGreeting> execute(UUID elderUserId) {
        UUID elderId = careAccessQuery.elderIdForUser(elderUserId);
        LocalDate today = clock.today();
        return greetingQuery.findFor(elderId, today);
    }
}
