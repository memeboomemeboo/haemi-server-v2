package com.memeboo2.haemi.elder.response.application;

import com.memeboo2.haemi.common.event.ElderResponded;
import com.memeboo2.haemi.common.event.VoiceResponseCreated;
import com.memeboo2.haemi.common.security.ElderAccessChecked;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.elder.response.domain.Emotion;
import com.memeboo2.haemi.elder.response.domain.Response;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.api.ElderMemoryQuery;
import com.memeboo2.haemi.platform.api.MediaPurpose;
import com.memeboo2.haemi.elder.response.domain.ResponseType;
import com.memeboo2.haemi.elder.response.infrastructure.ResponseRepository;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateResponseUseCase {

    private final ResponseRepository responseRepository;
    private final MediaUploadCommand mediaUploadCommand;
    private final ApplicationEventPublisher eventPublisher;
    private final CareAccessQuery careAccessQuery;
    private final ElderMemoryQuery elderMemoryQuery;
    private final HaemiClock clock;

    /** 마음 전하기 */
    @Transactional
    @ElderAccessChecked
    public UUID emotion(UUID elderUserId, UUID memoryId, List<Emotion> emotions) {
        UUID elderId = requireTargetMemory(elderUserId, memoryId);
        Response r = Response.emotion(memoryId, elderId, emotions);
        r = responseRepository.save(r);
        eventPublisher.publishEvent(new ElderResponded(memoryId, elderId, clock.today()));
        return r.getId();
    }

    /** 텍스트 댓글 */
    @Transactional
    @ElderAccessChecked
    public UUID text(UUID elderUserId, UUID memoryId, String text) {
        UUID elderId = requireTargetMemory(elderUserId, memoryId);
        Response r = Response.text(memoryId, elderId, text);
        r = responseRepository.save(r);
        eventPublisher.publishEvent(new ElderResponded(memoryId, elderId, clock.today()));
        return r.getId();
    }

    /** 이미지 댓글 */
    @Transactional
    @ElderAccessChecked
    public UUID image(UUID elderUserId, UUID memoryId, UUID mediaRefId) {
        UUID elderId = requireTargetMemory(elderUserId, memoryId);
        String mediaKey = mediaUploadCommand.confirmUpload(elderUserId, mediaRefId, MediaPurpose.RESPONSE_IMAGE).toString();
        Response r = Response.image(memoryId, elderId, mediaKey);
        r = responseRepository.save(r);
        eventPublisher.publishEvent(new ElderResponded(memoryId, elderId, clock.today()));
        return r.getId();
    }

    /** 음성 메시지 */
    @Transactional
    @ElderAccessChecked
    public UUID voice(UUID elderUserId, UUID memoryId, UUID mediaRefId) {
        UUID elderId = requireTargetMemory(elderUserId, memoryId);
        String mediaKey = mediaUploadCommand.confirmUpload(elderUserId, mediaRefId, MediaPurpose.RESPONSE_VOICE).toString();
        Integer durationSeconds = mediaUploadCommand.declaredDurationSeconds(mediaRefId);
        Response r = Response.voice(memoryId, elderId, mediaKey, durationSeconds);
        r = responseRepository.save(r);
        eventPublisher.publishEvent(new ElderResponded(memoryId, elderId, clock.today()));
        eventPublisher.publishEvent(new VoiceResponseCreated(r.getId(), mediaRefId));
        return r.getId();
    }

    private UUID requireTargetMemory(UUID elderUserId, UUID memoryId) {
        UUID elderId = careAccessQuery.elderIdForUser(elderUserId);
        if (elderMemoryQuery.findForElder(memoryId, elderId).isEmpty()) {
            throw new DomainException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return elderId;
    }
}
