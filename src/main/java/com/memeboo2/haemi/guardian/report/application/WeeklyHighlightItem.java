package com.memeboo2.haemi.guardian.report.application;

import java.util.UUID;

/** 디자인 카드 한 장에 대응하는 이번 주 하이라이트 항목이다. */
public record WeeklyHighlightItem(UUID id, String title, String body) {}
