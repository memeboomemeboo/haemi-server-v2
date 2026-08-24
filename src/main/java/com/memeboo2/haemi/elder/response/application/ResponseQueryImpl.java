package com.memeboo2.haemi.elder.response.application;

import com.memeboo2.haemi.guardian.api.ResponseQuery;
import com.memeboo2.haemi.elder.response.infrastructure.ResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResponseQueryImpl implements ResponseQuery {

    private final ResponseRepository responseRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ResponseItem> findByMemoryId(UUID memoryId) {
        return responseRepository.findByMemoryId(memoryId).stream()
                .map(r -> new ResponseItem(
                        r.getId(),
                        r.getResponseType().name(),
                        r.getEmotions().stream().map(Enum::name).toList(),
                        r.getText(),
                        r.getMediaKey()
                ))
                .toList();
    }
}
