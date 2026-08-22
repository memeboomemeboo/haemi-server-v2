package com.memeboo2.haemi.platform.media.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import com.memeboo2.haemi.platform.media.domain.MediaRef;
import com.memeboo2.haemi.platform.media.infrastructure.MediaRefRepository;
import com.memeboo2.haemi.platform.media.infrastructure.StoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConfirmUploadUseCase implements MediaUploadCommand {

    private final MediaRefRepository repository;
    private final StoragePort storage;
    private final HaemiClock clock;

    @Override
    @Transactional
    public URI confirmUpload(UUID actorId, UUID mediaRefId) {
        MediaRef ref = repository.findById(mediaRefId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!ref.isOwnedBy(actorId)) {
            throw new DomainException(ErrorCode.NOT_RESOURCE_OWNER);
        }

        ref.confirm(clock.now());

        return storage.generateServingUrl(ref.getStorageKey());
    }
}
