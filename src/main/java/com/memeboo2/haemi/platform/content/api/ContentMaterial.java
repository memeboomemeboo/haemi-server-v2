package com.memeboo2.haemi.platform.content.api;

import java.util.List;
import java.util.UUID;

/** CIST 문항에 사용할 큐레이션 콘텐츠의 공개 조회 모델이다. */
public record ContentMaterial(
        UUID id,
        String title,
        String imageKey,
        Integer contentYear,
        List<String> answerKeywords
) {}
