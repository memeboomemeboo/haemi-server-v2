package com.memeboo2.haemi.guardian.report.presentation;

import com.memeboo2.haemi.guardian.report.api.CognitiveArea;
import com.memeboo2.haemi.guardian.report.api.CognitiveStatus;
import com.memeboo2.haemi.guardian.report.api.CognitiveStatusQuery;
import com.memeboo2.haemi.guardian.report.presentation.dto.CognitiveStatusResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CognitiveStatusResponseTest {

    @Test
    @DisplayName("CognitiveStatusView로부터 elderId와 영역별 상태를 매핑한다")
    void from_전체_필드를_매핑한다() {
        UUID elderId = UUID.randomUUID();
        CognitiveStatusQuery.AreaStatus areaStatus =
                new CognitiveStatusQuery.AreaStatus(CognitiveArea.ORIENTATION, CognitiveStatus.WATCH, true);

        CognitiveStatusQuery.CognitiveStatusView view =
                new CognitiveStatusQuery.CognitiveStatusView(elderId, List.of(areaStatus));

        CognitiveStatusResponse response = CognitiveStatusResponse.from(view);

        assertThat(response.elderId()).isEqualTo(elderId);
        assertThat(response.areas()).hasSize(1);
        assertThat(response.areas().get(0).area()).isEqualTo(CognitiveArea.ORIENTATION);
        assertThat(response.areas().get(0).status()).isEqualTo(CognitiveStatus.WATCH);
        assertThat(response.areas().get(0).fourWeekDecline()).isTrue();
    }

    @Test
    @DisplayName("여러 영역이 있으면 순서를 유지하며 모두 매핑한다")
    void from_다중_영역을_매핑한다() {
        UUID elderId = UUID.randomUUID();
        List<CognitiveStatusQuery.AreaStatus> areas = List.of(
                new CognitiveStatusQuery.AreaStatus(CognitiveArea.RECALL, CognitiveStatus.GOOD, false),
                new CognitiveStatusQuery.AreaStatus(CognitiveArea.LANGUAGE, CognitiveStatus.NOT_AVAILABLE, false)
        );
        CognitiveStatusQuery.CognitiveStatusView view =
                new CognitiveStatusQuery.CognitiveStatusView(elderId, areas);

        CognitiveStatusResponse response = CognitiveStatusResponse.from(view);

        assertThat(response.areas()).extracting("area")
                .containsExactly(CognitiveArea.RECALL, CognitiveArea.LANGUAGE);
        assertThat(response.areas()).extracting("status")
                .containsExactly(CognitiveStatus.GOOD, CognitiveStatus.NOT_AVAILABLE);
    }
}
