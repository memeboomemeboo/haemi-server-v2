package com.memeboo2.haemi.elder.inbox.application;

import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.api.GreetingReadCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarkReadUseCase {

    private final GreetingReadCommand greetingReadCommand;
    private final CareAccessQuery careAccessQuery;

    public void execute(UUID elderId, UUID dailyCareId) {
        careAccessQuery.requireSelf(elderId, elderId);
        greetingReadCommand.markRead(elderId, dailyCareId);
    }
}
