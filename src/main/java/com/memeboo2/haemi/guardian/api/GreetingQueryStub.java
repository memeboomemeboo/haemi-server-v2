package com.memeboo2.haemi.guardian.api;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** guardian/dailycare 실구현 전까지 사용하는 빈 구현체. */
@Component
@ConditionalOnMissingBean(name = "greetingQueryImpl")
public class GreetingQueryStub implements GreetingQuery {

    @Override
    public List<ReceivedGreeting> findFor(UUID elderId, LocalDate date) {
        return List.of();
    }
}
