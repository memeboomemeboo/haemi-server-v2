package com.memeboo2.haemi.guardian.eldermanagement.application;

import com.memeboo2.haemi.guardian.api.ElderQuery;
import com.memeboo2.haemi.guardian.eldermanagement.domain.ElderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ElderQueryImpl implements ElderQuery {

    private final ElderRepository elderRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<ElderInfo> findById(UUID elderId) {
        return elderRepository.findById(elderId)
                .map(e -> new ElderInfo(e.getId(), e.getName(), e.getCreatedAt()));
    }
}
