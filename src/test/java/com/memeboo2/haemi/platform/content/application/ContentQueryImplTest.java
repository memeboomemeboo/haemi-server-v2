package com.memeboo2.haemi.platform.content.application;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.platform.content.api.ContentMaterial;
import com.memeboo2.haemi.platform.content.domain.ContentItem;
import com.memeboo2.haemi.platform.content.infrastructure.ContentExposureRepository;
import com.memeboo2.haemi.platform.content.infrastructure.ContentItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/** CIST 콘텐츠 쿨다운과 고갈 시 재사용 예외를 고정한다. */
@ExtendWith(MockitoExtension.class)
class ContentQueryImplTest {

    private static final Instant NOW = Instant.parse("2026-08-25T00:00:00Z");

    @Mock ContentItemRepository itemRepository;
    @Mock ContentExposureRepository exposureRepository;
    @Mock HaemiClock clock;

    @Test
    void 풀이_임계값_이상이면_최근_7일_노출_콘텐츠를_제외한다() {
        ContentItem recent = idOnlyItem(UUID.randomUUID());
        ContentItem available = materialItem("새 콘텐츠", UUID.randomUUID());
        UUID recentId = recent.getId();
        UUID availableId = available.getId();
        given(clock.now()).willReturn(NOW);
        given(itemRepository.findEligible("KR", 75, NOW)).willReturn(List.of(recent, available));
        given(exposureRepository.findContentIdsExposedSince(any(), any())).willReturn(List.of(recentId));
        given(exposureRepository.findByElderId(any())).willReturn(List.of());
        ContentQueryImpl query = new ContentQueryImpl(
                itemRepository, exposureRepository, new ContentPolicyProperties(7, 2), clock);

        List<ContentMaterial> selected = query.selectForTraining(UUID.randomUUID(), 75, 2, Set.of());

        assertThat(selected).extracting(ContentMaterial::id).containsExactly(availableId);
        then(exposureRepository).should().saveAll(any());
    }

    @Test
    void 풀이_임계값_미만이면_최근_노출도_다시_사용한다() {
        ContentItem only = materialItem("재사용 콘텐츠", UUID.randomUUID());
        UUID onlyId = only.getId();
        given(clock.now()).willReturn(NOW);
        given(itemRepository.findEligible("KR", 75, NOW)).willReturn(List.of(only));
        given(exposureRepository.findContentIdsExposedSince(any(), any())).willReturn(List.of(onlyId));
        given(exposureRepository.findByElderId(any())).willReturn(List.of());
        ContentQueryImpl query = new ContentQueryImpl(
                itemRepository, exposureRepository, new ContentPolicyProperties(7, 2), clock);

        List<ContentMaterial> selected = query.selectForTraining(UUID.randomUUID(), 75, 1, Set.of());

        assertThat(selected).extracting(ContentMaterial::id).containsExactly(onlyId);
    }

    private ContentItem idOnlyItem(UUID id) {
        ContentItem item = org.mockito.Mockito.mock(ContentItem.class);
        given(item.getId()).willReturn(id);
        return item;
    }

    private ContentItem materialItem(String title, UUID id) {
        ContentItem item = idOnlyItem(id);
        given(item.getTitle()).willReturn(title);
        given(item.getImageKey()).willReturn("content/" + title + ".jpg");
        given(item.getContentYear()).willReturn(1970);
        given(item.keywordList()).willReturn(List.of(title));
        return item;
    }
}
