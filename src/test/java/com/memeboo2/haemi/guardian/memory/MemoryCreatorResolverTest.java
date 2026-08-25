package com.memeboo2.haemi.guardian.memory;

import com.memeboo2.haemi.auth.api.AccountQuery;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLink;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLinkRepository;
import com.memeboo2.haemi.guardian.memory.application.MemoryCreatorResolver;
import com.memeboo2.haemi.guardian.memory.application.MemoryWithCreator;
import com.memeboo2.haemi.guardian.memory.domain.Memory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MemoryCreatorResolverTest {

    @Mock AccountQuery accountQuery;
    @Mock GuardianElderLinkRepository linkRepository;
    @InjectMocks MemoryCreatorResolver resolver;

    UUID elderId = UUID.randomUUID();

    @Test
    void 링크_해제된_보호자가_만든_추억은_creatorRole이_null이다() {
        Memory memory = createMemoryWithCreator(elderId, UUID.randomUUID());
        UUID createdBy = memory.getCreatedBy();
        given(accountQuery.findById(createdBy))
                .willReturn(Optional.of(new AccountQuery.AccountInfo(createdBy, "황정빈", "id", "010", null, null, null)));
        given(linkRepository.findByGuardianIdAndElderId(createdBy, elderId)).willReturn(Optional.empty());

        MemoryWithCreator result = resolver.resolve(memory, UUID.randomUUID());

        assertThat(result.creatorName()).isEqualTo("황정빈");
        assertThat(result.creatorRole()).isNull();
    }

    @Test
    void 링크된_보호자는_이름과_역할을_반환한다() {
        Memory memory = createMemoryWithCreator(elderId, UUID.randomUUID());
        UUID createdBy = memory.getCreatedBy();
        given(accountQuery.findById(createdBy))
                .willReturn(Optional.of(new AccountQuery.AccountInfo(createdBy, "황정빈", "id", "010", null, null, null)));
        GuardianElderLink link = GuardianElderLink.create(createdBy, elderId);
        link.changeRole(GuardianRole.DAUGHTER);
        given(linkRepository.findByGuardianIdAndElderId(createdBy, elderId)).willReturn(Optional.of(link));

        MemoryWithCreator result = resolver.resolve(memory, createdBy);

        assertThat(result.creatorName()).isEqualTo("황정빈");
        assertThat(result.creatorRole()).isEqualTo(GuardianRole.DAUGHTER);
        assertThat(result.isMine()).isTrue();
    }

    @Test
    void 다른_보호자가_조회하면_isMine이_false다() {
        Memory memory = createMemoryWithCreator(elderId, UUID.randomUUID());
        UUID createdBy = memory.getCreatedBy();
        given(accountQuery.findById(createdBy)).willReturn(Optional.empty());
        given(linkRepository.findByGuardianIdAndElderId(createdBy, elderId)).willReturn(Optional.empty());

        MemoryWithCreator result = resolver.resolve(memory, UUID.randomUUID());

        assertThat(result.isMine()).isFalse();
    }

    @Test
    void 목록_조회는_생성자_계정과_역할을_일괄_조회한다() {
        UUID createdBy = UUID.randomUUID();
        Memory memory = createMemoryWithCreator(elderId, createdBy);
        given(accountQuery.findAllById(List.of(createdBy)))
                .willReturn(List.of(new AccountQuery.AccountInfo(createdBy, "황정빈", "id", "010", null, null, null)));
        GuardianElderLink link = GuardianElderLink.create(createdBy, elderId);
        link.changeRole(GuardianRole.SON);
        given(linkRepository.findAllByGuardianIdInAndElderId(List.of(createdBy), elderId)).willReturn(List.of(link));

        List<MemoryWithCreator> result = resolver.resolveAll(List.of(memory), elderId, createdBy);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).creatorName()).isEqualTo("황정빈");
        assertThat(result.get(0).creatorRole()).isEqualTo(GuardianRole.SON);
        assertThat(result.get(0).isMine()).isTrue();
    }

    /** createdBy는 JPA Auditing(@CreatedBy)이 채우므로 리플렉션으로 세팅한다. */
    private Memory createMemoryWithCreator(UUID elderId, UUID createdBy) {
        Memory memory = Memory.create(elderId, "제목", null, "한마디", 2020);
        org.springframework.test.util.ReflectionTestUtils.setField(memory, "createdBy", createdBy);
        return memory;
    }
}
