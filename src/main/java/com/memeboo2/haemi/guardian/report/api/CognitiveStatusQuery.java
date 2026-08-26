package com.memeboo2.haemi.guardian.report.api;

import java.util.List;
import java.util.UUID;

/** RPT-ATT-004 결과를 RPT-ATT-005·006이 재사용하는 보호자 리포트 읽기 계약. */
public interface CognitiveStatusQuery {

    CognitiveStatusView cognitiveStatus(UUID guardianId, UUID elderId);

    /** 수치 정답률·원문 답변은 보호자 응답 계약 밖으로 숨긴다. */
    record CognitiveStatusView(UUID elderId, List<AreaStatus> areas) {
        public CognitiveStatusView {
            areas = List.copyOf(areas);
        }
    }

    record AreaStatus(CognitiveArea area, CognitiveStatus status, boolean fourWeekDecline) {}
}
