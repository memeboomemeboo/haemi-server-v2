package com.memeboo2.haemi.elder.api;

import java.util.List;
import java.util.UUID;

/**
 * 보호자가 어르신 답변 내용을 조회하기 위한 계약.
 * 소유: elder/response. guardian/memory 가 호출.
 */
public interface ResponseQuery {

    List<ResponseItem> findByMemoryId(UUID memoryId);

    record ResponseItem(
            UUID id,
            String responseType,
            List<String> emotions,
            String text,
            String mediaKey
    ) {}
}
