package com.memeboo2.haemi.guardian.dailycare.application;

import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.dailycare.domain.DailyCare;
import com.memeboo2.haemi.guardian.dailycare.infrastructure.DailyCareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** 보호자 본인이 보낸 하루 한마디 이력 조회 (R6: 발신자 본인 것만). */
@Service
@RequiredArgsConstructor
public class GetSentDailyCareHistoryUseCase {

    private final CareAccessQuery careAccessQuery;
    private final DailyCareRepository dailyCareRepository;

    @Transactional(readOnly = true)
    public List<DailyCare> execute(UUID guardianId, UUID elderId) {
        careAccessQuery.requireGuardianOf(guardianId, elderId);
        return dailyCareRepository.findByGuardianIdAndElderIdOrderByCareDateDescCreatedAtDesc(guardianId, elderId);
    }
}
