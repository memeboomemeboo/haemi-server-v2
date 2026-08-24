package com.memeboo2.haemi.guardian.home.presentation.dto;

import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.home.application.GetGuardianHomeUseCase.ElderCard;
import com.memeboo2.haemi.guardian.home.application.GetGuardianHomeUseCase.GuardianHomeData;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
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
            long daysTogether,
            boolean attendedToday,
            boolean greetingSentToday,
            @Schema(description = "어르신이 마지막으로 로그인한 시각. 접속 기록이 없으면 null")
            Instant lastLoginAt
    ) {
        static ElderCardResponse from(ElderCard card) {
            return new ElderCardResponse(
                    card.elderId(), card.name(), card.age(), card.role(), card.daysTogether(),
                    card.attendedToday(), card.greetingSentToday(), card.lastLoginAt());
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
