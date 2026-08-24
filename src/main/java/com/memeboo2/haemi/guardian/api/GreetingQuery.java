package com.memeboo2.haemi.guardian.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 어르신이 받은 하루 한마디 조회. elder/inbox 소비용.
 * 소유: guardian/dailycare. 황정빈-4 단계에서 실구현.
 */
public interface GreetingQuery {

    List<ReceivedGreeting> findFor(UUID elderId, LocalDate date);

    record ReceivedGreeting(
        UUID id,
        UUID guardianId,
        String guardianName,
        GreetingContent content,
        boolean read
    ) {}

    sealed interface GreetingContent permits GreetingContent.Text, GreetingContent.Voice {
        record Text(String message) implements GreetingContent {}
        record Voice(String mediaKey, int durationSeconds) implements GreetingContent {}
    }
}
