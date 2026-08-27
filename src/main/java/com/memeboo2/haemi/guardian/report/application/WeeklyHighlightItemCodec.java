package com.memeboo2.haemi.guardian.report.application;

import java.util.List;
import java.util.UUID;

/** 기존 줄 단위 저장값을 읽을 수 있도록 하이라이트 항목을 단일 DB 컬럼에 안정적으로 보관한다. */
final class WeeklyHighlightItemCodec {

    private static final String ITEM_SEPARATOR = "\u001E";
    private static final String FIELD_SEPARATOR = "\u001F";
    private static final String DEFAULT_TITLE = "이번 주 하이라이트";

    private WeeklyHighlightItemCodec() {}

    static String encode(List<WeeklyHighlightItem> items) {
        return items.stream()
                .map(item -> item.id() + FIELD_SEPARATOR + item.title() + FIELD_SEPARATOR + item.body())
                .reduce((left, right) -> left + ITEM_SEPARATOR + right)
                .orElse("");
    }

    static List<WeeklyHighlightItem> decode(String content) {
        if (content.indexOf(FIELD_SEPARATOR) < 0) {
            return WeekAnchor.splitLines(content).stream()
                    .map(line -> new WeeklyHighlightItem(UUID.randomUUID(), DEFAULT_TITLE, line))
                    .toList();
        }
        return List.of(content.split(ITEM_SEPARATOR, -1)).stream()
                .map(encoded -> encoded.split(FIELD_SEPARATOR, -1))
                .filter(fields -> fields.length == 3)
                .map(fields -> new WeeklyHighlightItem(UUID.fromString(fields[0]), fields[1], fields[2]))
                .toList();
    }
}
