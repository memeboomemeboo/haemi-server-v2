package com.memeboo2.haemi.guardian.api;

import java.util.List;
import java.util.UUID;

/** 보호자가 추억에 대한 어르신 답변을 조회하는 공개 계약. */
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
