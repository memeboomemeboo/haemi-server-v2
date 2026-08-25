package com.memeboo2.haemi.elder.memory;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.event.MemoryViewed;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.elder.memory.application.MarkMemoryViewedUseCase;
import com.memeboo2.haemi.elder.memory.infrastructure.MemoryViewRepository;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.api.ElderMemoryQuery;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MarkMemoryViewedUseCaseTest {

    @Mock ElderMemoryQuery elderMemoryQuery;
    @Mock CareAccessQuery careAccessQuery;
    @Mock MemoryViewRepository memoryViewRepository;
    @Mock org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock HaemiClock clock;
    @InjectMocks MarkMemoryViewedUseCase useCase;

    UUID elderUserId = UUID.randomUUID();
    UUID elderId = UUID.randomUUID();
    UUID memoryId = UUID.randomUUID();

    private ElderMemoryQuery.MemoryItem item() {
        return new ElderMemoryQuery.MemoryItem(memoryId, "추억", null, "한마디", null,
                List.of(), false, Instant.now(), "보호자", GuardianRole.GUARDIAN);
    }

    private void stubAccess() {
        given(careAccessQuery.elderIdForUser(elderUserId)).willReturn(elderId);
        given(elderMemoryQuery.findForElder(memoryId, elderId)).willReturn(Optional.of(item()));
    }

    @Test
    void 최초_열람이면_MemoryViewed를_발행한다() {
        stubAccess();
        given(clock.now()).willReturn(Instant.parse("2026-08-25T01:00:00Z"));
        given(clock.today()).willReturn(LocalDate.of(2026, 8, 25));
        given(memoryViewRepository.insertIfAbsent(elderId, memoryId, clock.now())).willReturn(1);

        useCase.execute(elderUserId, memoryId);

        ArgumentCaptor<MemoryViewed> captor = ArgumentCaptor.forClass(MemoryViewed.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().elderId()).isEqualTo(elderId);
        assertThat(captor.getValue().memoryId()).isEqualTo(memoryId);
        assertThat(captor.getValue().viewedDate()).isEqualTo(LocalDate.of(2026, 8, 25));
    }

    @Test
    void 재열람이면_이벤트를_발행하지_않는다() {
        stubAccess();
        given(clock.now()).willReturn(Instant.parse("2026-08-25T01:00:00Z"));
        given(memoryViewRepository.insertIfAbsent(elderId, memoryId, clock.now())).willReturn(0);

        useCase.execute(elderUserId, memoryId);

        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 본인_추억이_아니면_404() {
        given(careAccessQuery.elderIdForUser(elderUserId)).willReturn(elderId);
        given(elderMemoryQuery.findForElder(memoryId, elderId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(elderUserId, memoryId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 권한없는_접근은_403() {
        given(careAccessQuery.elderIdForUser(elderUserId)).willReturn(elderId);
        willThrow(new DomainException(ErrorCode.CARE_ACCESS_DENIED))
                .given(careAccessQuery).requireSelf(elderUserId, elderId);

        assertThatThrownBy(() -> useCase.execute(elderUserId, memoryId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CARE_ACCESS_DENIED));
    }
}
