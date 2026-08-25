package com.memeboo2.haemi.elder.training.domain;

import java.util.List;
import java.util.UUID;

/** 문항이 참조하는 앨범 또는 큐레이션 콘텐츠의 훈련 내부 모델이다. */
public record TrainingMaterial(
        UUID id,
        MaterialSource source,
        String title,
        String imageKey,
        Integer year,
        List<String> keywords
) {}
