package com.memeboo2.haemi.guardian.home.application;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.dailycare.infrastructure.DailyCareRepository;
import com.memeboo2.haemi.guardian.eldermanagement.domain.Elder;
import com.memeboo2.haemi.guardian.eldermanagement.domain.ElderRepository;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLinkRepository;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetGuardianHomeUseCase {

    private final CareAccessQuery careAccessQuery;
    private final GuardianElderLinkRepository linkRepository;
    private final ElderRepository elderRepository;
    private final DailyCareRepository dailyCareRepository;
    private final MemoryRepository memoryRepository;
    private final HaemiClock clock;

    @Transactional(readOnly = true)
    public GuardianHomeData execute(UUID guardianId) {
        List<UUID> elderIds = careAccessQuery.accessibleElders(guardianId);
        var today = clock.today();
        var now = clock.now();

        List<ElderCard> cards = elderIds.stream().map(elderId -> {
            Elder elder = elderRepository.findById(elderId).orElse(null);
            if (elder == null) return null;
            GuardianRole role = careAccessQuery.roleOf(guardianId, elderId);
            boolean greetingSentToday = dailyCareRepository
                    .existsByGuardianIdAndElderIdAndCareDate(guardianId, elderId, today);
            return new ElderCard(elderId, elder.getName(), elder.getBirthDate(), role, greetingSentToday);
        }).filter(c -> c != null).toList();

        boolean memoryRegisteredToday = memoryRepository
                .existsByCreatedByAndCreatedAtAfter(guardianId, today.atStartOfDay()
                        .atZone(java.time.ZoneOffset.UTC).toInstant());

        boolean allGreetingsSent = !cards.isEmpty() &&
                cards.stream().allMatch(ElderCard::greetingSentToday);

        return new GuardianHomeData(cards, new Challenge(allGreetingsSent, memoryRegisteredToday));
    }

    public record ElderCard(
            UUID elderId,
            String name,
            java.time.LocalDate birthDate,
            GuardianRole role,
            boolean greetingSentToday
    ) {}

    public record Challenge(boolean greetingCompleted, boolean memoryCompleted) {}

    public record GuardianHomeData(List<ElderCard> elders, Challenge challenge) {}
}
