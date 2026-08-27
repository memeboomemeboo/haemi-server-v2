package com.memeboo2.haemi.guardian.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 보호자가 추억에 대한 어르신 답변을 조회하는 공개 계약. */
public interface ResponseQuery {

    List<ResponseItem> findByMemoryId(UUID memoryId);

    /** 오늘의 기록 타임라인(#100 M2): 어르신이 지정 구간에 남긴 답변들(도착 시각순 정렬은 호출측). */
    List<ElderResponseActivity> findByElderIdBetween(UUID elderId, Instant from, Instant to);

    record ElderResponseActivity(
            UUID memoryId,
            String responseType,
            String text,
            String transcript,
            Instant createdAt
    ) {}

    record ResponseItem(
            UUID id,
            String responseType,
            List<String> emotions,
            String text,
            String mediaKey,
            /** 음성 답변 전사(STT). 미전사·비음성이면 null (#100 X3) */
            String transcript,
            /** 답변 작성 시각 — 상세 화면의 "2일전·오후 3:20" 표시용 (#100 X3) */
            Instant createdAt
    ) {}
}
