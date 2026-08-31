package com.memeboo2.haemi.elder.inbox.application;

import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.common.security.ElderAccessChecked;
import com.memeboo2.haemi.guardian.api.GreetingReadCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarkReadUseCase {

    private final GreetingReadCommand greetingReadCommand;
    private final CareAccessQuery careAccessQuery;

    @ElderAccessChecked
    public void execute(UUID elderUserId, UUID dailyCareId) {
        UUID elderId = careAccessQuery.elderIdForUser(elderUserId);
        greetingReadCommand.markRead(elderId, dailyCareId);
    }
}
