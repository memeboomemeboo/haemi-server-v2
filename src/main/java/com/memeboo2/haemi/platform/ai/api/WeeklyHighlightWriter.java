package com.memeboo2.haemi.platform.ai.api;

import java.util.List;

/** RPT-ATT-005의 사실 기반 하이라이트 문구 생성 포트. */
public interface WeeklyHighlightWriter {

    /** 1~3줄의 보호자용, 비진단적 문구를 생성한다. */
    List<String> write(WeeklyHighlightPrompt prompt);
}
