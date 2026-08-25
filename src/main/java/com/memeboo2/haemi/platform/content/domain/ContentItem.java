package com.memeboo2.haemi.platform.content.domain;

import com.memeboo2.haemi.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "platform_content_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentItem extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String imageKey;

    @Column
    private Integer contentYear;

    /** 쉼표로 보관하고 API 경계에서만 목록으로 변환한다. */
    @Column(nullable = false, length = 500)
    private String answerKeywords;

    @Column(nullable = false, length = 20)
    private String region;

    @Column
    private Integer recommendedMinAge;

    @Column
    private Integer recommendedMaxAge;

    @Column
    private Instant availableUntil;

    public static ContentItem create(
            String title,
            String imageKey,
            Integer contentYear,
            List<String> answerKeywords,
            String region,
            Integer recommendedMinAge,
            Integer recommendedMaxAge,
            Instant availableUntil
    ) {
        ContentItem item = new ContentItem();
        item.assignIdIfAbsent();
        item.title = title;
        item.imageKey = imageKey;
        item.contentYear = contentYear;
        item.answerKeywords = String.join(",", answerKeywords);
        item.region = region;
        item.recommendedMinAge = recommendedMinAge;
        item.recommendedMaxAge = recommendedMaxAge;
        item.availableUntil = availableUntil;
        return item;
    }

    public List<String> keywordList() {
        return Arrays.stream(answerKeywords.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
