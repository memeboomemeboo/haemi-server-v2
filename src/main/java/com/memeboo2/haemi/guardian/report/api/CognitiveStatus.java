package com.memeboo2.haemi.guardian.report.api;

/** 점수 없이 노출하는 RPT-ATT-004 영역 상태. */
public enum CognitiveStatus {
    GOOD,
    NORMAL,
    WATCH,
    /** 최근 7일에 자동 채점 가능한 응답이 없는 영역. 색상 판정을 억지로 만들지 않는다. */
    NOT_AVAILABLE
}
