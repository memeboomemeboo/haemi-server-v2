package com.memeboo2.haemi.guardian.eldermanagement.access;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.api.ElderProfileQuery;
import com.memeboo2.haemi.guardian.eldermanagement.domain.Elder;
import com.memeboo2.haemi.guardian.eldermanagement.domain.ElderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ElderProfileQueryImpl implements ElderProfileQuery {

    private final ElderRepository elderRepository;

    @Override
    public ElderProfile findById(UUID elderId) {
        Elder elder = elderRepository.findById(elderId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, "어르신을 찾을 수 없습니다."));
        return new ElderProfile(elder.getBirthDate(), elder.getCreatedAt());
    }
}
