package com.memeboo2.haemi.guardian.dailycare.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.event.GreetingSent;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.dailycare.domain.DailyCare;
import com.memeboo2.haemi.guardian.dailycare.infrastructure.DailyCareRepository;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import com.memeboo2.haemi.platform.api.MediaPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SendDailyCareUseCase {

    private final DailyCareRepository dailyCareRepository;
    private final CareAccessQuery careAccessQuery;
    private final DailyCareProperties props;
    private final HaemiClock clock;
    private final ApplicationEventPublisher publisher;
    private final MediaUploadCommand mediaUploadCommand;

    @Transactional
    public UUID sendText(UUID guardianId, UUID elderId, String text) {
        careAccessQuery.requireGuardianOf(guardianId, elderId);
        LocalDate today = clock.today();
        checkDuplicate(guardianId, elderId, today);

        DailyCare care = DailyCare.text(guardianId, elderId, today, text, props.retentionDays());
        saveDailyCare(care);
        publisher.publishEvent(new GreetingSent(care.getId(), guardianId, elderId, today));
        return care.getId();
    }

    @Transactional
    public UUID sendVoice(UUID guardianId, UUID elderId, UUID mediaRefId, int durationSeconds) {
        careAccessQuery.requireGuardianOf(guardianId, elderId);
        if (durationSeconds > props.maxVoiceDurationSeconds()) {
            throw new DomainException(ErrorCode.INVALID_INPUT,
                    "음성은 " + props.maxVoiceDurationSeconds() + "초 이하입니다.");
        }
        LocalDate today = clock.today();
        checkDuplicate(guardianId, elderId, today);

        String servingUrl = mediaUploadCommand.confirmUpload(
                guardianId, mediaRefId, MediaPurpose.GREETING_VOICE, durationSeconds).toString();
        DailyCare care = DailyCare.voice(guardianId, elderId, today, servingUrl, durationSeconds, props.retentionDays());
        saveDailyCare(care);
        publisher.publishEvent(new GreetingSent(care.getId(), guardianId, elderId, today));
        return care.getId();
    }

    private void checkDuplicate(UUID guardianId, UUID elderId, LocalDate date) {
        if (dailyCareRepository.existsByGuardianIdAndElderIdAndCareDate(guardianId, elderId, date)) {
            throw new DomainException(ErrorCode.DAILY_CARE_ALREADY_SENT,
                    "오늘은 이미 하루 한마디를 전했습니다.");
        }
    }

    private void saveDailyCare(DailyCare care) {
        try {
            dailyCareRepository.saveAndFlush(care);
        } catch (DataIntegrityViolationException ex) {
            throw new DomainException(ErrorCode.DAILY_CARE_ALREADY_SENT,
                    "오늘은 이미 하루 한마디를 전했습니다.");
        }
    }
}
