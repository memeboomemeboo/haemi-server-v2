package com.memeboo2.haemi.guardian.home.application;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.api.ResponseQuery;
import com.memeboo2.haemi.guardian.api.TrainingActivityQuery;
import com.memeboo2.haemi.guardian.dailycare.domain.DailyCare;
import com.memeboo2.haemi.guardian.dailycare.infrastructure.DailyCareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 보호자 홈 "오늘의 기록" 타임라인(#100 M2).
 * 어르신의 하루 활동(인지 훈련 완료·추억 답변 도착·하루 한마디 열람)을 시각순으로 모은다.
 * 여러 모듈의 읽기 계약을 조합하되 수치 점수는 노출하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class GetTodayActivitiesUseCase {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final CareAccessQuery careAccessQuery;
    private final TrainingActivityQuery trainingActivityQuery;
    private final ResponseQuery responseQuery;
    private final DailyCareRepository dailyCareRepository;
    private final HaemiClock clock;

    public enum ActivityKind { COGNITIVE_TRAINING, MEMORY_RESPONSE, DAILY_CARE_READ }

    public record ActivityEntry(Instant at, ActivityKind kind, String summary, UUID memoryId) {}

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
                entries.add(new ActivityEntry(s.completedAt(), ActivityKind.COGNITIVE_TRAINING,
                        "인지 활동 완료", null)));

        responseQuery.findByElderIdBetween(elderId, from, to).forEach(r ->
                entries.add(new ActivityEntry(r.createdAt(), ActivityKind.MEMORY_RESPONSE,
                        responseSummary(r), r.memoryId())));

        dailyCareRepository.findByElderIdAndDate(elderId, date, now).stream()
                .filter(c -> c.getViewedAt() != null
                        && !c.getViewedAt().isBefore(from) && c.getViewedAt().isBefore(to))
                .forEach(c -> entries.add(new ActivityEntry(c.getViewedAt(), ActivityKind.DAILY_CARE_READ,
                        "하루 한마디 열람", null)));

        entries.sort(Comparator.comparing(ActivityEntry::at));
        return entries;
    }

    private String responseSummary(ResponseQuery.ElderResponseActivity r) {
        if (r.transcript() != null && !r.transcript().isBlank()) {
            return "음성 메시지 도착 · " + r.transcript();
        }
        if (r.text() != null && !r.text().isBlank()) {
            return "메시지 도착 · " + r.text();
        }
        return switch (r.responseType()) {
            case "VOICE" -> "음성 메시지 도착";
            case "EMOTION" -> "마음 전하기 도착";
            case "IMAGE" -> "사진 답변 도착";
            default -> "답변 도착";
        };
    }
}
