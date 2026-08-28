package com.memeboo2.haemi.guardian.report.application;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

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

    static List<WeeklyHighlightItem> decode(String content, UUID elderId, LocalDate weekStart) {
        if (content.indexOf(FIELD_SEPARATOR) < 0) {
            return generatedItems(elderId, weekStart, WeekAnchor.splitLines(content));
        }
        return List.of(content.split(ITEM_SEPARATOR, -1)).stream()
                .map(encoded -> encoded.split(FIELD_SEPARATOR, -1))
                .filter(fields -> fields.length == 3)
                .map(fields -> new WeeklyHighlightItem(UUID.fromString(fields[0]), fields[1], fields[2]))
                .toList();
    }

    /**
     * 자동 생성·레거시 줄 단위 문구에는 DB에 저장된 식별자가 없다.
     * 같은 어르신의 같은 주·같은 카드 순서에는 항상 같은 UUID를 돌려줘 조회 후 편집을 안정화한다.
     */
    static List<WeeklyHighlightItem> generatedItems(UUID elderId, LocalDate weekStart, List<String> lines) {
        return IntStream.range(0, lines.size())
                .mapToObj(index -> new WeeklyHighlightItem(stableGeneratedId(elderId, weekStart, index),
                        DEFAULT_TITLE, lines.get(index)))
                .toList();
    }

    private static UUID stableGeneratedId(UUID elderId, LocalDate weekStart, int index) {
        String source = "weekly-highlight:" + elderId + ':' + weekStart + ':' + index;
        return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
    }
}
