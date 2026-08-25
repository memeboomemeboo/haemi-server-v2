package com.memeboo2.haemi.guardian.dailycare.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 하루 한마디 전송 (R6).
 *
 * <p>적재 본문은 {@link DailyCareSaver}가 REQUIRES_NEW 트랜잭션에서 수행한다 —
 * uk_daily_care_guardian_elder_date 위반이 나도 그 트랜잭션에만 갇혀 깨끗한 409로 나간다.
 */
@Service
@RequiredArgsConstructor
public class SendDailyCareUseCase {

    private final CareAccessQuery careAccessQuery;
    private final DailyCareProperties props;
    private final HaemiClock clock;
    private final DailyCareSaver dailyCareSaver;

    public UUID sendText(UUID guardianId, UUID elderId, String text) {
        careAccessQuery.requireGuardianOf(guardianId, elderId);
        return dailyCareSaver.saveText(guardianId, elderId, clock.today(), text);
    }

    public UUID sendVoice(UUID guardianId, UUID elderId, UUID mediaRefId, int durationSeconds) {
        careAccessQuery.requireGuardianOf(guardianId, elderId);
        if (durationSeconds > props.maxVoiceDurationSeconds()) {
            throw new DomainException(ErrorCode.INVALID_INPUT,
                    "음성은 " + props.maxVoiceDurationSeconds() + "초 이하입니다.");
        }
        return dailyCareSaver.saveVoice(guardianId, elderId, clock.today(), mediaRefId, durationSeconds);
    }
}
