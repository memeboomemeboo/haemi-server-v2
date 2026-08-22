package com.memeboo2.haemi.guardian.home.presentation.dto;

import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.home.application.GetGuardianHomeUseCase.ElderCard;
import com.memeboo2.haemi.guardian.home.application.GetGuardianHomeUseCase.GuardianHomeData;

import java.time.LocalDate;
import java.time.Period;
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
            boolean greetingSentToday
    ) {
        static ElderCardResponse from(ElderCard card) {
            Integer age = card.birthDate() != null
                    ? Period.between(card.birthDate(), LocalDate.now()).getYears()
                    : null;
            return new ElderCardResponse(card.elderId(), card.name(), age, card.role(), card.greetingSentToday());
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
