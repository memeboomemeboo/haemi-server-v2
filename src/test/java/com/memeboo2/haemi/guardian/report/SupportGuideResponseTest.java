package com.memeboo2.haemi.guardian.report;

import com.memeboo2.haemi.guardian.report.application.GetSupportGuideUseCase.SupportGuide;
import com.memeboo2.haemi.guardian.report.application.GetSupportGuideUseCase.Suggestion;
import com.memeboo2.haemi.guardian.report.application.SupportGuideAction;
import com.memeboo2.haemi.guardian.report.presentation.dto.SupportGuideResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SupportGuideResponseTest {

    @Test
    void from_mapsElderNameAndSuggestions() {
        SupportGuide guide = new SupportGuide("김할머니", List.of(
                new Suggestion(SupportGuideAction.SEND_DAILY_CARE, "하루 한마디를 보내보세요."),
                new Suggestion(SupportGuideAction.CALL_ELDER, "안부 전화를 해보세요.")
        ));

        SupportGuideResponse response = SupportGuideResponse.from(guide);

        assertThat(response.elderName()).isEqualTo("김할머니");
        assertThat(response.suggestions()).hasSize(2);
        assertThat(response.suggestions().get(0).action()).isEqualTo(SupportGuideAction.SEND_DAILY_CARE);
        assertThat(response.suggestions().get(0).message()).isEqualTo("하루 한마디를 보내보세요.");
        assertThat(response.suggestions().get(1).action()).isEqualTo(SupportGuideAction.CALL_ELDER);
        assertThat(response.suggestions().get(1).message()).isEqualTo("안부 전화를 해보세요.");
    }

    @Test
    void from_emptySuggestions_returnsEmptyList() {
        SupportGuide guide = new SupportGuide("박할아버지", List.of());

        SupportGuideResponse response = SupportGuideResponse.from(guide);

        assertThat(response.elderName()).isEqualTo("박할아버지");
        assertThat(response.suggestions()).isEmpty();
    }
}
