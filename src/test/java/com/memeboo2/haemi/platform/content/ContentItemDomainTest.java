package com.memeboo2.haemi.platform.content;

import com.memeboo2.haemi.platform.content.domain.ContentItem;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** ContentItem의 create 팩토리와 키워드 목록 변환을 검증한다. */
class ContentItemDomainTest {

    @Test
    void create는_전달받은_값으로_컨텐츠를_생성한다() {
        Instant availableUntil = Instant.parse("2026-12-31T00:00:00Z");

        ContentItem item = ContentItem.create(
                "설날", "image-key", 1990, List.of("설날", "떡국"), "KR", 60, 90, availableUntil);

        assertThat(item.getId()).isNotNull();
        assertThat(item.getTitle()).isEqualTo("설날");
        assertThat(item.getImageKey()).isEqualTo("image-key");
        assertThat(item.getContentYear()).isEqualTo(1990);
        assertThat(item.getRegion()).isEqualTo("KR");
        assertThat(item.getRecommendedMinAge()).isEqualTo(60);
        assertThat(item.getRecommendedMaxAge()).isEqualTo(90);
        assertThat(item.getAvailableUntil()).isEqualTo(availableUntil);
    }

    @Test
    void create는_호출할_때마다_새로운_id를_부여한다() {
        ContentItem first = ContentItem.create("A", "key1", null, List.of("a"), "KR", null, null, null);
        ContentItem second = ContentItem.create("B", "key2", null, List.of("b"), "KR", null, null, null);

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @Test
    void keywordList는_쉼표로_결합된_키워드를_목록으로_변환한다() {
        ContentItem item = ContentItem.create(
                "설날", "image-key", 1990, List.of("설날", "떡국", "세배"), "KR", null, null, null);

        assertThat(item.keywordList()).containsExactly("설날", "떡국", "세배");
    }

    @Test
    void keywordList는_키워드가_하나면_단일_목록을_반환한다() {
        ContentItem item = ContentItem.create(
                "제목", "image-key", null, List.of("단일키워드"), "KR", null, null, null);

        assertThat(item.keywordList()).containsExactly("단일키워드");
    }

    @Test
    void keywordList는_빈_키워드를_필터링한다() {
        ContentItem item = ContentItem.create(
                "제목", "image-key", null, List.of("keyword1", "", "keyword2"), "KR", null, null, null);

        assertThat(item.keywordList()).containsExactly("keyword1", "keyword2");
    }

    @Test
    void 선택적_필드는_null이어도_생성할_수_있다() {
        ContentItem item = ContentItem.create(
                "제목", "image-key", null, List.of("keyword"), "KR", null, null, null);

        assertThat(item.getContentYear()).isNull();
        assertThat(item.getRecommendedMinAge()).isNull();
        assertThat(item.getRecommendedMaxAge()).isNull();
        assertThat(item.getAvailableUntil()).isNull();
    }
}
