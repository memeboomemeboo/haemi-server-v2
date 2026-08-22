package com.memeboo2.haemi.guardian.dailycare.application;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.GreetingQuery;
import com.memeboo2.haemi.guardian.dailycare.domain.CareType;
import com.memeboo2.haemi.guardian.dailycare.domain.DailyCare;
import com.memeboo2.haemi.guardian.dailycare.infrastructure.DailyCareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service("greetingQueryImpl")
@RequiredArgsConstructor
public class GreetingQueryImpl implements GreetingQuery {

    private final DailyCareRepository dailyCareRepository;
    private final HaemiClock clock;

    @Override
    @Transactional(readOnly = true)
    public List<ReceivedGreeting> findFor(UUID elderId, LocalDate date) {
        return dailyCareRepository.findByElderIdAndDate(elderId, date, clock.now())
                .stream().map(this::toReceived).toList();
    }

    private ReceivedGreeting toReceived(DailyCare c) {
        GreetingContent content = c.getCareType() == CareType.TEXT
                ? new GreetingContent.Text(c.getText())
                : new GreetingContent.Voice(c.getMediaKey(), c.getDurationSeconds());
        return new ReceivedGreeting(c.getId(), c.getGuardianId(), null, content, c.isRead());
    }
}
