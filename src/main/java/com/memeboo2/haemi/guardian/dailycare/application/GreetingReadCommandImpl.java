package com.memeboo2.haemi.guardian.dailycare.application;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.GreetingReadCommand;
import com.memeboo2.haemi.guardian.dailycare.infrastructure.DailyCareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GreetingReadCommandImpl implements GreetingReadCommand {

    private final DailyCareRepository dailyCareRepository;
    private final HaemiClock clock;

    @Override
    @Transactional
    public void markRead(UUID elderId, UUID dailyCareId) {
        dailyCareRepository.findById(dailyCareId)
                .filter(c -> c.getElderId().equals(elderId))
                .ifPresent(c -> c.markViewed(clock.now()));
    }
}
