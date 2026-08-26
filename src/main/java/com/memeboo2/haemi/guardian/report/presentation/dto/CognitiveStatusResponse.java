package com.memeboo2.haemi.guardian.report.presentation.dto;

import com.memeboo2.haemi.guardian.report.api.CognitiveArea;
import com.memeboo2.haemi.guardian.report.api.CognitiveStatus;
import com.memeboo2.haemi.guardian.report.api.CognitiveStatusQuery;

import java.util.List;
import java.util.UUID;

/** RPT-ATT-004 응답. 정답률·점수 대신 영역별 상태와 관찰 신호만 공개한다. */
public record CognitiveStatusResponse(
        UUID elderId,
        List<AreaStatusResponse> areas
) {
    public record AreaStatusResponse(
            CognitiveArea area,
            CognitiveStatus status,
            boolean fourWeekDecline
    ) {
        private static AreaStatusResponse from(CognitiveStatusQuery.AreaStatus areaStatus) {
            return new AreaStatusResponse(
                    areaStatus.area(), areaStatus.status(), areaStatus.fourWeekDecline());
        }
    }

    public static CognitiveStatusResponse from(CognitiveStatusQuery.CognitiveStatusView view) {
        return new CognitiveStatusResponse(
                view.elderId(), view.areas().stream().map(AreaStatusResponse::from).toList());
    }
}
