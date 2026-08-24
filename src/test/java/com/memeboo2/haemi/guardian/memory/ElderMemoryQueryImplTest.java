package com.memeboo2.haemi.guardian.memory;

import com.memeboo2.haemi.auth.api.AccountQuery;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLinkRepository;
import com.memeboo2.haemi.guardian.memory.application.ElderMemoryQueryImpl;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ElderMemoryQueryImplTest {

    @Mock MemoryRepository memoryRepository;
    @Mock AccountQuery accountQuery;
    @Mock GuardianElderLinkRepository linkRepository;
    @Mock HaemiClock clock;
    @InjectMocks ElderMemoryQueryImpl query;

    @Test
    void 어르신_추억_상세도_최근_1년_범위로_조회한다() {
        UUID memoryId = UUID.randomUUID();
        UUID elderId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-23T00:00:00Z");
        given(clock.now()).willReturn(now);
        given(memoryRepository.findByIdAndElderIdSinceWithImages(
                memoryId, elderId, now.minusSeconds(365L * 24 * 3600))).willReturn(Optional.empty());

        query.findForElder(memoryId, elderId);

        verify(memoryRepository).findByIdAndElderIdSinceWithImages(
                memoryId, elderId, now.minusSeconds(365L * 24 * 3600));
    }
}
