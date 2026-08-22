package com.memeboo2.haemi.elder.response.application;

import com.memeboo2.haemi.common.event.ElderResponded;
import com.memeboo2.haemi.elder.response.domain.Emotion;
import com.memeboo2.haemi.elder.response.domain.Response;
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

    /** 마음 전하기 */
    @Transactional
    public UUID emotion(UUID elderId, UUID memoryId, List<Emotion> emotions) {
        Response r = Response.emotion(memoryId, elderId, emotions);
        r = responseRepository.save(r);
        eventPublisher.publishEvent(new ElderResponded(memoryId, elderId));
        return r.getId();
    }

    /** 텍스트 댓글 */
    @Transactional
    public UUID text(UUID elderId, UUID memoryId, String text) {
        Response r = Response.text(memoryId, elderId, text);
        r = responseRepository.save(r);
        eventPublisher.publishEvent(new ElderResponded(memoryId, elderId));
        return r.getId();
    }

    /** 이미지 댓글 */
    @Transactional
    public UUID image(UUID elderId, UUID memoryId, UUID mediaRefId) {
        String mediaKey = mediaUploadCommand.confirmUpload(elderId, mediaRefId).toString();
        Response r = Response.image(memoryId, elderId, mediaKey);
        r = responseRepository.save(r);
        eventPublisher.publishEvent(new ElderResponded(memoryId, elderId));
        return r.getId();
    }

    /** 음성 메시지 */
    @Transactional
    public UUID voice(UUID elderId, UUID memoryId, UUID mediaRefId) {
        String mediaKey = mediaUploadCommand.confirmUpload(elderId, mediaRefId).toString();
        Response r = Response.voice(memoryId, elderId, mediaKey);
        r = responseRepository.save(r);
        eventPublisher.publishEvent(new ElderResponded(memoryId, elderId));
        return r.getId();
    }
}
