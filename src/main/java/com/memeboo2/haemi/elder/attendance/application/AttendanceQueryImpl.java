package com.memeboo2.haemi.elder.attendance.application;

import com.memeboo2.haemi.common.attendance.StreakCalculator;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.elder.attendance.infrastructure.DailyParticipationRepository;
import com.memeboo2.haemi.guardian.api.AttendanceQuery;
import com.memeboo2.haemi.guardian.api.ElderQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.stream.Collectors;

/** AttendanceQueryStub을 대체하는 실구현. bean name이 "attendanceQueryImpl"이어야 한다. */
@Service("attendanceQueryImpl")
@RequiredArgsConstructor
public class AttendanceQueryImpl implements AttendanceQuery {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DailyParticipationRepository repository;
    private final ElderQuery elderQuery;
    private final HaemiClock clock;

    @Override
    @Transactional(readOnly = true)
    public boolean completedToday(UUID elderId) {
        return repository.existsByElderIdAndParticipationDate(elderId, clock.today());
    }

    @Override
    @Transactional(readOnly = true)
    public int currentStreak(UUID elderId) {
        var dates = repository.findByElderId(elderId).stream()
                .map(p -> p.getParticipationDate())
                .collect(Collectors.toSet());
        return StreakCalculator.currentStreak(dates, clock.today());
    }

    @Override
    @Transactional(readOnly = true)
    public long daysTogether(UUID elderId) {
        return elderQuery.findById(elderId)
                .map(info -> {
                    LocalDate registeredDate = info.registeredAt().atZone(KST).toLocalDate();
                    return ChronoUnit.DAYS.between(registeredDate, clock.today());
                })
                .orElse(0L);
    }
}
