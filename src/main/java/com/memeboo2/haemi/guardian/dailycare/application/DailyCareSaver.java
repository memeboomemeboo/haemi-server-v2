package com.memeboo2.haemi.guardian.dailycare.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.event.GreetingSent;
import com.memeboo2.haemi.common.persistence.ConstraintViolations;
import com.memeboo2.haemi.guardian.dailycare.domain.DailyCare;
import com.memeboo2.haemi.guardian.dailycare.infrastructure.DailyCareRepository;
import com.memeboo2.haemi.platform.api.MediaPurpose;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 하루 한마디 적재를 별도 트랜잭션(REQUIRES_NEW)에 가둔다.
 *
 * <p>중복 전송의 마지막 방어선은 {@code uk_daily_care_guardian_elder_date} 유니크 제약이다 —
 * 같은 보호자가 같은 어르신에게 동시에 전송하면 선검사를 함께 통과하고, 그때 이 제약이 잡는다.
 * 그런데 이 위반을 <b>바깥 트랜잭션과 같은 커넥션</b>에서 잡으면 Postgres가 트랜잭션 전체를
 * abort시켜, catch 후 정상 흐름을 이어가거나 바깥이 커밋을 시도할 때 500이 된다.
 * REQUIRES_NEW로 분리하면 abort가 이 트랜잭션에 갇히고, {@code DomainException}으로 빠져나가
 * 호출자는 깨끗한 409를 받는다.
 *
 * <p>미디어 확정과 적재를 <b>같은 트랜잭션</b>에서 수행하므로, 중복으로 되돌아갈 때
 * 음성 확정도 함께 취소된다.
 */
@Component
@RequiredArgsConstructor
public class DailyCareSaver {

    private final DailyCareRepository dailyCareRepository;
    private final DailyCareProperties props;
    private final ApplicationEventPublisher publisher;
    private final MediaUploadCommand mediaUploadCommand;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID saveText(UUID guardianId, UUID elderId, LocalDate today, String text) {
        requireNotSentToday(guardianId, elderId, today);
        return persist(DailyCare.text(guardianId, elderId, today, text, props.retentionDays()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID saveVoice(UUID guardianId, UUID elderId, LocalDate today,
                          UUID mediaRefId, int durationSeconds) {
        requireNotSentToday(guardianId, elderId, today);
        String storageKey = mediaUploadCommand.confirmUploadKey(
                guardianId, mediaRefId, MediaPurpose.GREETING_VOICE, durationSeconds);
        return persist(DailyCare.voice(guardianId, elderId, today, storageKey, durationSeconds,
                props.retentionDays()));
    }

    /** 흔한 경우를 선검사로 거른다. 동시 전송은 아래 유니크 제약이 최종적으로 잡는다. */
    private void requireNotSentToday(UUID guardianId, UUID elderId, LocalDate date) {
        if (dailyCareRepository.existsByGuardianIdAndElderIdAndCareDate(guardianId, elderId, date)) {
            throw alreadySent();
        }
    }

    private UUID persist(DailyCare care) {
        try {
            dailyCareRepository.saveAndFlush(care);
        } catch (DataIntegrityViolationException violation) {
            if (ConstraintViolations.isViolationOf(violation, "uk_daily_care_guardian_elder_date")) {
                throw alreadySent();
            }
            throw violation;
        }
        publisher.publishEvent(
                new GreetingSent(care.getId(), care.getGuardianId(), care.getElderId(), care.getCareDate()));
        return care.getId();
    }

    private DomainException alreadySent() {
        return new DomainException(ErrorCode.DAILY_CARE_ALREADY_SENT, "오늘은 이미 하루 한마디를 전했습니다.");
    }
}
