package com.memeboo2.haemi.elder.home.application;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.elder.api.AttendanceQuery;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.api.ElderMemoryQuery;
import com.memeboo2.haemi.guardian.api.ElderMemoryQuery.MemoryItem;
import com.memeboo2.haemi.guardian.api.GreetingQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetElderHomeUseCase {

    private static final int RECENT_MEMORY_LIMIT = 3;

    private final GreetingQuery greetingQuery;
    private final ElderMemoryQuery elderMemoryQuery;
    private final AttendanceQuery attendanceQuery;
    private final CareAccessQuery careAccessQuery;
    private final HaemiClock clock;

    public ElderHomeData execute(UUID elderId) {
        careAccessQuery.requireSelf(elderId, elderId);

        var greetings = greetingQuery.findFor(elderId, clock.today());
        long unreadCount = greetings.stream().filter(g -> !g.read()).count();

        List<MemoryItem> recentMemories = elderMemoryQuery.listForElder(elderId)
                .stream().limit(RECENT_MEMORY_LIMIT).toList();

        boolean trainedToday = attendanceQuery.completedToday(elderId);
        int streak = attendanceQuery.currentStreak(elderId);

        return new ElderHomeData(
                (long) greetings.size(),
                unreadCount,
                recentMemories,
                trainedToday,
                streak
        );
    }

    public record ElderHomeData(
            long todayGreetingCount,
            long unreadGreetingCount,
            List<MemoryItem> recentMemories,
            boolean trainedToday,
            int streak
    ) {}
}
