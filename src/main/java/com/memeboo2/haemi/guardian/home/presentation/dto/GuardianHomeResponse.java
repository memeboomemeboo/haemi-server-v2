package com.memeboo2.haemi.guardian.home.presentation.dto;

import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.home.application.GetGuardianHomeUseCase.ElderCard;
import com.memeboo2.haemi.guardian.home.application.GetGuardianHomeUseCase.GuardianHomeData;

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
            boolean greetingSentToday
    ) {
        static ElderCardResponse from(ElderCard card) {
            return new ElderCardResponse(
                    card.elderId(), card.name(), card.age(), card.role(), card.role().getLabel(),
                    card.daysTogether(), card.attendedToday(), card.greetingSentToday());
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
