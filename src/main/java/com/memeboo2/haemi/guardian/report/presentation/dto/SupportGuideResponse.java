package com.memeboo2.haemi.guardian.report.presentation.dto;

import com.memeboo2.haemi.guardian.report.application.GetSupportGuideUseCase.SupportGuide;
import com.memeboo2.haemi.guardian.report.application.GetSupportGuideUseCase.Suggestion;
import com.memeboo2.haemi.guardian.report.application.SupportGuideAction;

import java.util.List;

/** RPT-ATT-006 응답. action은 클라이언트가 기존 기능으로 연결할 때 사용한다. */
public record SupportGuideResponse(
        String elderName,
        List<SuggestionResponse> suggestions
) {
    public record SuggestionResponse(SupportGuideAction action, String message) {
        private static SuggestionResponse from(Suggestion suggestion) {
            return new SuggestionResponse(suggestion.action(), suggestion.message());
        }
    }

    public static SupportGuideResponse from(SupportGuide guide) {
        return new SupportGuideResponse(
                guide.elderName(),
                guide.suggestions().stream().map(SuggestionResponse::from).toList()
        );
    }
}
