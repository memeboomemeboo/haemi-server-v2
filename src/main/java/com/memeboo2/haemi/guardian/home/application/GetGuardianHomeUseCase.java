package com.memeboo2.haemi.guardian.home.application;

import com.memeboo2.haemi.auth.api.AccountQuery;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.api.AttendanceQuery;
import com.memeboo2.haemi.guardian.report.api.CognitiveStatusQuery;
import com.memeboo2.haemi.guardian.dailycare.infrastructure.DailyCareRepository;
import com.memeboo2.haemi.guardian.eldermanagement.domain.Elder;
import com.memeboo2.haemi.guardian.eldermanagement.domain.ElderRepository;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetGuardianHomeUseCase {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final CareAccessQuery careAccessQuery;
    private final ElderRepository elderRepository;
    private final DailyCareRepository dailyCareRepository;
    private final MemoryRepository memoryRepository;
    private final AttendanceQuery attendanceQuery;
    private final AccountQuery accountQuery;
    private final CognitiveStatusQuery cognitiveStatusQuery;
    private final HaemiClock clock;

    @Transactional(readOnly = true)
    public GuardianHomeData execute(UUID guardianId) {
        List<UUID> elderIds = careAccessQuery.accessibleElders(guardianId);
        var today = clock.today();

        List<ElderCard> cards = elderIds.stream().map(elderId -> {
            Elder elder = elderRepository.findById(elderId).orElse(null);
            if (elder == null) return null;
            GuardianRole role = careAccessQuery.roleOf(guardianId, elderId);
            boolean greetingSentToday = dailyCareRepository
                    .existsByGuardianIdAndElderIdAndCareDate(guardianId, elderId, today);
            Integer age = elder.getBirthDate() == null ? null : Period.between(elder.getBirthDate(), today).getYears();
            Instant lastLoginAt = accountQuery.findById(elder.getUserId())
                    .map(AccountQuery.AccountInfo::lastLoginAt).orElse(null);
            GuardianCondition condition = overallCondition(
                    cognitiveStatusQuery.cognitiveStatus(guardianId, elderId));
            return new ElderCard(
                    elderId, elder.getName(), age, role,
                    attendanceQuery.daysTogether(elderId), attendanceQuery.completedToday(elderId),
                    greetingSentToday, lastLoginAt, condition,
                    attendanceQuery.weeklyActivities(elderId));
        }).filter(c -> c != null).toList();

        boolean memoryRegisteredToday = memoryRepository
                .existsByCreatedByAndCreatedAtAfter(guardianId, today.atStartOfDay()
                        .atZone(KST).toInstant());

        boolean allGreetingsSent = !cards.isEmpty() &&
                cards.stream().allMatch(ElderCard::greetingSentToday);

        return new GuardianHomeData(cards, new Challenge(allGreetingsSent, memoryRegisteredToday));
    }

    /**
     * 디자인 기준 "오늘 컨디션"은 인지 영역 가운데 가장 낮은 상태로 요약한다.
     * 판정 데이터가 전혀 없으면 임의의 3색 상태를 만들지 않고 null을 반환한다.
     */
    private GuardianCondition overallCondition(CognitiveStatusQuery.CognitiveStatusView view) {
        GuardianCondition worst = null;
        for (var area : view.areas()) {
            worst = switch (area.status()) {
                case WATCH -> GuardianCondition.OBSERVE;
                case NORMAL -> worst == GuardianCondition.OBSERVE ? worst : GuardianCondition.CAUTION;
                case GOOD -> worst == null ? GuardianCondition.GOOD : worst;
                case NOT_AVAILABLE -> worst;
            };
        }
        return worst;
    }

    /** 디자인 홈 카드의 3색 컨디션. 리포트의 NORMAL/WATCH와 구별해 노출한다. */
    public enum GuardianCondition { GOOD, CAUTION, OBSERVE }

    public record ElderCard(
            UUID elderId,
            String name,
            Integer age,
            GuardianRole role,
            long daysTogether,
            boolean attendedToday,
            boolean greetingSentToday,
            Instant lastLoginAt,
            GuardianCondition condition,
            List<AttendanceQuery.DayActivity> weeklyActivities
    ) {
    }

    public record Challenge(boolean greetingCompleted, boolean memoryCompleted) {}

    public record GuardianHomeData(List<ElderCard> elders, Challenge challenge) {}
}
