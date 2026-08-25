package com.memeboo2.haemi.platform.content.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** CIST가 재료 부족 시 사용하는 큐레이션 콘텐츠 선택 계약이다. */
public interface ContentQuery {

    List<ContentMaterial> selectForTraining(UUID elderId, Integer age, int limit, Set<UUID> excludedContentIds);
}
