package com.memeboo2.haemi.guardian.memory;

import com.memeboo2.haemi.auth.api.AccountQuery;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLink;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLinkRepository;
import com.memeboo2.haemi.guardian.memory.application.ElderMemoryQueryImpl;
import com.memeboo2.haemi.guardian.memory.domain.Memory;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void 생성자_관계가_기타이면_보호자로_치환한다_D17() {
        UUID elderId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-23T00:00:00Z");
        given(clock.now()).willReturn(now);

        Memory memory = Memory.create(elderId, "제목", null, "한마디", 2020);
        ReflectionTestUtils.setField(memory, "createdBy", creatorId);
        given(memoryRepository.findByElderIdSince(elderId, now.minusSeconds(365L * 24 * 3600)))
                .willReturn(List.of(memory));
        given(accountQuery.findById(creatorId))
                .willReturn(Optional.of(new AccountQuery.AccountInfo(creatorId, "황정빈", "id", "010", null, null)));
        GuardianElderLink link = GuardianElderLink.create(creatorId, elderId);
        link.changeRole(GuardianRole.기타);
        given(linkRepository.findByGuardianIdAndElderId(creatorId, elderId)).willReturn(Optional.of(link));

        var result = query.listForElder(elderId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).creatorRole()).isEqualTo(GuardianRole.보호자);
    }
}
