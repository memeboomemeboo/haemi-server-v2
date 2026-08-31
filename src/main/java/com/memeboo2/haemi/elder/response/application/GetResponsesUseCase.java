package com.memeboo2.haemi.elder.response.application;

import com.memeboo2.haemi.elder.response.domain.Response;
import com.memeboo2.haemi.common.security.ElderAccessChecked;
import com.memeboo2.haemi.elder.response.infrastructure.ResponseRepository;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.api.ElderMemoryQuery;
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
    private final CareAccessQuery careAccessQuery;
    private final ElderMemoryQuery elderMemoryQuery;

    @Transactional(readOnly = true)
    @ElderAccessChecked
    public List<Response> execute(UUID elderUserId, UUID memoryId) {
        UUID elderId = careAccessQuery.elderIdForUser(elderUserId);
        if (elderMemoryQuery.findForElder(memoryId, elderId).isEmpty()) {
            throw new DomainException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return responseRepository.findByMemoryIdAndElderId(memoryId, elderId);
    }
}
