package com.memeboo2.haemi.elder.response.application;

import com.memeboo2.haemi.elder.response.domain.Response;
import com.memeboo2.haemi.elder.response.infrastructure.ResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** 어르신 본인이 자기 답변 목록을 조회 (R9: 본인 수신분만) */
@Service
@RequiredArgsConstructor
public class GetResponsesUseCase {

    private final ResponseRepository responseRepository;

    @Transactional(readOnly = true)
    public List<Response> execute(UUID elderId, UUID memoryId) {
        return responseRepository.findByMemoryIdAndElderId(memoryId, elderId);
    }
}
