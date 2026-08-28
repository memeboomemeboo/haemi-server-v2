package com.memeboo2.haemi.guardian.home.application;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.api.MemoryViewActivityQuery;
import com.memeboo2.haemi.guardian.api.ResponseQuery;
import com.memeboo2.haemi.guardian.api.TrainingActivityQuery;
import com.memeboo2.haemi.guardian.dailycare.domain.DailyCare;
import com.memeboo2.haemi.guardian.dailycare.infrastructure.DailyCareRepository;
import com.memeboo2.haemi.guardian.memory.domain.Memory;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 보호자 홈 "오늘의 기록" 타임라인(#100 M2).
 * 어르신의 하루 활동을 디자인 계약의 유형·상세 정보와 함께 시각순으로 모은다.
 */
@Service
@RequiredArgsConstructor
public class GetTodayActivitiesUseCase {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final CareAccessQuery careAccessQuery;
    private final TrainingActivityQuery trainingActivityQuery;
    private final ResponseQuery responseQuery;
    private final MemoryViewActivityQuery memoryViewActivityQuery;
    private final DailyCareRepository dailyCareRepository;
    private final MemoryRepository memoryRepository;
    private final HaemiClock clock;

    public enum ActivityType { TRAINING_COMPLETED, GREETING_ARRIVED, GREETING_READ, MEMORY_VIEWED, RESPONSE_SENT }

    public record ActivityEntry(Instant occurredAt, ActivityType type, String title, Map<String, Object> detail) {}

    public LocalDate today() {
        return clock.today();
    }

    /** date 미지정("오늘") 요청용. */
    @Transactional(readOnly = true)
    public List<ActivityEntry> executeToday(UUID guardianId, UUID elderId) {
        return execute(guardianId, elderId, clock.today());
    }

    @Transactional(readOnly = true)
    public List<ActivityEntry> execute(UUID guardianId, UUID elderId, LocalDate date) {
        careAccessQuery.requireGuardianOf(guardianId, elderId);

        Instant now = clock.now();
        Instant from = date.atStartOfDay(KST).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(KST).toInstant();

        List<ActivityEntry> entries = new ArrayList<>();

        trainingActivityQuery.completedOn(elderId, date).forEach(s ->
                entries.add(new ActivityEntry(s.completedAt(), ActivityType.TRAINING_COMPLETED,
                        "인지 활동 완료", Map.of(
                                "activityName", "인지 훈련",
                                "durationMinutes", s.durationMinutes(),
                                "accuracy", s.accuracy()))));

        responseQuery.findByElderIdBetween(elderId, from, to).forEach(r ->
                entries.add(new ActivityEntry(r.createdAt(), ActivityType.RESPONSE_SENT,
                        "추억 답변 완료", Map.of(
                                "memoryId", r.memoryId(),
                                "responseType", r.responseType()))));

        dailyCareRepository.findByElderIdAndDate(elderId, date, now).forEach(c -> {
            if (isWithin(c.getCreatedAt(), from, to)) {
                entries.add(new ActivityEntry(c.getCreatedAt(), ActivityType.GREETING_ARRIVED,
                        "하루 한마디 도착", greetingDetail(c)));
            }
            if (isWithin(c.getViewedAt(), from, to)) {
                entries.add(new ActivityEntry(c.getViewedAt(), ActivityType.GREETING_READ,
                        "하루 한마디 열람", Map.of()));
            }
        });

        List<MemoryViewActivityQuery.MemoryViewActivity> memoryViews =
                memoryViewActivityQuery.firstViewedBetween(elderId, from, to);
        Map<UUID, String> titles = memoryRepository.findAllById(memoryViews.stream()
                        .map(MemoryViewActivityQuery.MemoryViewActivity::memoryId).toList())
                .stream()
                .collect(Collectors.toMap(Memory::getId, Memory::getTitle));
        memoryViews.forEach(view -> {
            Map<String, Object> detail = new java.util.HashMap<>();
            detail.put("memoryId", view.memoryId());
            if (titles.containsKey(view.memoryId())) detail.put("memoryTitle", titles.get(view.memoryId()));
            entries.add(new ActivityEntry(view.firstViewedAt(), ActivityType.MEMORY_VIEWED,
                    "추억 열람", detail));
        });

        entries.sort(Comparator.comparing(ActivityEntry::occurredAt));
        return entries;
    }

    private Map<String, Object> greetingDetail(DailyCare care) {
        Map<String, Object> detail = new java.util.HashMap<>();
        detail.put("medium", care.getCareType().name());
        if (care.getText() != null && !care.getText().isBlank()) detail.put("preview", care.getText());
        if (care.getDurationSeconds() != null) detail.put("durationSeconds", care.getDurationSeconds());
        return detail;
    }

    private boolean isWithin(Instant occurredAt, Instant from, Instant to) {
        return occurredAt != null && !occurredAt.isBefore(from) && occurredAt.isBefore(to);
    }
}
