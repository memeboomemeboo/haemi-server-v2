package com.memeboo2.haemi.elder.reminiscence.domain;

import com.memeboo2.haemi.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/** 매일 08:00 배치로 어르신별 생성되는 개인화 회상 콘텐츠. (elderId, contentDate) 당 하나. */
@Entity
@Table(name = "ai_reminiscence_contents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GeneratedReminiscence extends BaseEntity {

    @Column(nullable = false)
    private UUID elderId;

    @Column(nullable = false)
    private LocalDate contentDate;

    @Column(nullable = false, length = 2000)
    private String content;

    /** 실제 LLM 생성 여부 (false = 템플릿 대체). */
    @Column(nullable = false)
    private boolean aiGenerated;

    public static GeneratedReminiscence of(UUID elderId, LocalDate contentDate, String content, boolean aiGenerated) {
        GeneratedReminiscence r = new GeneratedReminiscence();
        r.assignIdIfAbsent();
        r.elderId = elderId;
        r.contentDate = contentDate;
        r.content = content;
        r.aiGenerated = aiGenerated;
        return r;
    }

    public void update(String content, boolean aiGenerated) {
        this.content = content;
        this.aiGenerated = aiGenerated;
    }
}
