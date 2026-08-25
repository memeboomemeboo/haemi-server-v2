package com.memeboo2.haemi.guardian.dailycare;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.event.GreetingRead;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.dailycare.application.GreetingReadCommandImpl;
import com.memeboo2.haemi.guardian.dailycare.domain.DailyCare;
import com.memeboo2.haemi.guardian.dailycare.infrastructure.DailyCareRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GreetingReadCommandImplTest {

    @Mock DailyCareRepository dailyCareRepository;
    @Mock org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock HaemiClock clock;
    @InjectMocks GreetingReadCommandImpl command;

    UUID elderId = UUID.randomUUID();
    UUID dailyCareId = UUID.randomUUID();

    private DailyCare careFor(UUID owner) {
        return DailyCare.text(UUID.randomUUID(), owner, LocalDate.of(2026, 8, 25), "안녕하세요", 30);
    }

    @Test
    void 최초_열람이면_상태를_바꾸고_이벤트를_발행한다() {
        DailyCare care = careFor(elderId);
        given(dailyCareRepository.findById(dailyCareId)).willReturn(Optional.of(care));
        given(clock.now()).willReturn(Instant.parse("2026-08-25T01:00:00Z"));
        given(clock.toLocalDate(Instant.parse("2026-08-25T01:00:00Z"))).willReturn(LocalDate.of(2026, 8, 25));

        command.markRead(elderId, dailyCareId);

        assertThat(care.isRead()).isTrue();
        ArgumentCaptor<GreetingRead> captor = ArgumentCaptor.forClass(GreetingRead.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().elderId()).isEqualTo(elderId);
        assertThat(captor.getValue().dailyCareId()).isEqualTo(dailyCareId);
        assertThat(captor.getValue().readDate()).isEqualTo(LocalDate.of(2026, 8, 25));
    }

    @Test
    void 이미_읽은_한마디는_이벤트를_발행하지_않는다() {
        DailyCare care = careFor(elderId);
        care.markViewed(Instant.parse("2026-08-24T01:00:00Z")); // 이미 읽음
        given(dailyCareRepository.findById(dailyCareId)).willReturn(Optional.of(care));
        lenient().when(clock.now()).thenReturn(Instant.parse("2026-08-25T01:00:00Z"));

        command.markRead(elderId, dailyCareId);

        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 존재하지_않으면_404() {
        given(dailyCareRepository.findById(dailyCareId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> command.markRead(elderId, dailyCareId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void 다른_어르신의_항목이면_403() {
        DailyCare othersCare = careFor(UUID.randomUUID());
        given(dailyCareRepository.findById(dailyCareId)).willReturn(Optional.of(othersCare));

        assertThatThrownBy(() -> command.markRead(elderId, dailyCareId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CARE_ACCESS_DENIED));
    }
}
