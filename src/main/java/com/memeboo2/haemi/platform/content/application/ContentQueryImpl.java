package com.memeboo2.haemi.platform.content.application;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.platform.content.api.ContentMaterial;
import com.memeboo2.haemi.platform.content.api.ContentQuery;
import com.memeboo2.haemi.platform.content.domain.ContentExposure;
import com.memeboo2.haemi.platform.content.domain.ContentItem;
import com.memeboo2.haemi.platform.content.infrastructure.ContentExposureRepository;
import com.memeboo2.haemi.platform.content.infrastructure.ContentItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 최근 노출 콘텐츠를 피하고, 풀이 작으면 쿨다운을 해제하는 CIST 콘텐츠 선택기다. */
@Service
@RequiredArgsConstructor
public class ContentQueryImpl implements ContentQuery {

    private static final String KOREA = "KR";

    private final ContentItemRepository contentItemRepository;
    private final ContentExposureRepository contentExposureRepository;
    private final ContentPolicyProperties policy;
    private final HaemiClock clock;

    @Override
    @Transactional
    public List<ContentMaterial> selectForTraining(UUID elderId, Integer age, int limit, Set<UUID> excludedContentIds) {
        Instant now = clock.now();
        List<ContentItem> eligible = contentItemRepository.findEligible(KOREA, age, now);
        Set<UUID> recentlyExposed = new HashSet<>(contentExposureRepository.findContentIdsExposedSince(
                elderId, now.minus(Duration.ofDays(policy.cooldownDays()))));
        Map<UUID, Instant> lastExposedAt = contentExposureRepository.findLatestExposuresByElderId(elderId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        ContentExposureRepository.LatestExposure::getContentId,
                        ContentExposureRepository.LatestExposure::getExposedAt));

        List<ContentItem> selectable = eligible.stream()
                .filter(item -> !excludedContentIds.contains(item.getId()))
                .toList();
        List<ContentItem> notRecentlyExposed = selectable.stream()
                .filter(item -> !recentlyExposed.contains(item.getId()))
                .toList();
        List<ContentItem> candidates = (notRecentlyExposed.size() < policy.depletionThreshold()
                ? selectable : notRecentlyExposed).stream()
                .sorted(Comparator
                        .comparing((ContentItem item) -> lastExposedAt.get(item.getId()),
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(ContentItem::getCreatedAt))
                .limit(limit)
                .toList();

        contentExposureRepository.saveAll(candidates.stream()
                .map(item -> ContentExposure.record(elderId, item.getId(), now))
                .toList());

        return candidates.stream()
                .map(item -> new ContentMaterial(
                        item.getId(), item.getTitle(), item.getImageKey(), item.getContentYear(), item.keywordList()))
                .toList();
    }
}
