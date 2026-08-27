package com.memeboo2.haemi.guardian.home.presentation.dto;

import com.memeboo2.haemi.guardian.api.AttendanceQuery;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.home.application.GetGuardianHomeUseCase.ElderCard;
import com.memeboo2.haemi.guardian.home.application.GetGuardianHomeUseCase.GuardianHomeData;
import com.memeboo2.haemi.guardian.home.application.GetGuardianHomeUseCase.GuardianCondition;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GuardianHomeResponse(
        List<ElderCardResponse> elders,
        ChallengeResponse challenge
) {
    public record ElderCardResponse(
            UUID elderId,
            String name,
            Integer age,
            GuardianRole role,
            String roleLabel,
            long daysTogether,
            boolean attendedToday,
            boolean greetingSentToday,
            @Schema(description = "어르신이 마지막으로 로그인한 시각. 접속 기록이 없으면 null")
            Instant lastLoginAt,
            @Schema(description = "오늘 컨디션 요약(GOOD/CAUTION/OBSERVE). 판정 데이터가 없으면 null")
            GuardianCondition condition,
            @Schema(description = "최근 7일 요일별 활동 종류 완료 여부 (스택 막대용). 과거→오늘 순")
            List<DayActivityResponse> weeklyActivities
    ) {
        static ElderCardResponse from(ElderCard card) {
            return new ElderCardResponse(
                    card.elderId(), card.name(), card.age(), card.role(), card.role().getLabel(),
                    card.daysTogether(), card.attendedToday(), card.greetingSentToday(), card.lastLoginAt(),
                    card.condition(),
                    card.weeklyActivities().stream().map(DayActivityResponse::from).toList());
        }
    }

    public record DayActivityResponse(
            LocalDate date, DayOfWeek dayOfWeek,
            boolean training, boolean greetingRead, boolean memoryViewed, boolean replied
    ) {
        static DayActivityResponse from(AttendanceQuery.DayActivity a) {
            return new DayActivityResponse(a.date(), a.dayOfWeek(),
                    a.training(), a.greetingRead(), a.memoryViewed(), a.replied());
        }
    }

    public record ChallengeResponse(boolean greetingCompleted, boolean memoryCompleted) {}

    public static GuardianHomeResponse from(GuardianHomeData data) {
        return new GuardianHomeResponse(
                data.elders().stream().map(ElderCardResponse::from).toList(),
                new ChallengeResponse(data.challenge().greetingCompleted(), data.challenge().memoryCompleted())
        );
    }
}
