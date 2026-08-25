package com.memeboo2.haemi.guardian.dailycare.application;

import com.memeboo2.haemi.auth.api.AccountQuery;
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
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service("greetingQueryImpl")
@RequiredArgsConstructor
public class GreetingQueryImpl implements GreetingQuery {

    private final DailyCareRepository dailyCareRepository;
    private final AccountQuery accountQuery;
    private final HaemiClock clock;

    @Override
    @Transactional(readOnly = true)
    public List<ReceivedGreeting> findFor(UUID elderId, LocalDate date) {
        List<DailyCare> cares = dailyCareRepository.findByElderIdAndDate(elderId, date, clock.now());

        // 발신 보호자 이름을 일괄 조회 (N+1 방지)
        List<UUID> guardianIds = cares.stream().map(DailyCare::getGuardianId).distinct().toList();
        Map<UUID, String> guardianNames = accountQuery.findAllById(guardianIds).stream()
                .collect(Collectors.toMap(AccountQuery.AccountInfo::userId, AccountQuery.AccountInfo::name,
                        (a, b) -> a));

        return cares.stream()
                .map(c -> toReceived(c, guardianNames.get(c.getGuardianId())))
                .toList();
    }

    private ReceivedGreeting toReceived(DailyCare c, String guardianName) {
        GreetingContent content = c.getCareType() == CareType.TEXT
                ? new GreetingContent.Text(c.getText())
                : new GreetingContent.Voice(c.getMediaKey(), c.getDurationSeconds());
        return new ReceivedGreeting(c.getId(), c.getGuardianId(), guardianName, content, c.isRead());
    }
}
