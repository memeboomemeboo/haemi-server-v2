package com.memeboo2.haemi.elder.training;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.event.TrainingSessionCompleted;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.elder.training.application.CompleteTrainingSessionUseCase;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CompleteTrainingSessionUseCaseTest {

    @Mock CareAccessQuery careAccessQuery;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock HaemiClock clock;
    @InjectMocks CompleteTrainingSessionUseCase useCase;

    UUID elderUserId = UUID.randomUUID();
    UUID elderId = UUID.randomUUID();
    LocalDate today = LocalDate.of(2026, 8, 25);

    @Test
    void 완료하면_도메인_elderId와_오늘_날짜로_이벤트를_발행한다() {
        given(careAccessQuery.elderIdForUser(elderUserId)).willReturn(elderId);
        given(clock.today()).willReturn(today);

        assertThat(useCase.completeToday(elderUserId)).isEqualTo(today);

        ArgumentCaptor<TrainingSessionCompleted> captor = ArgumentCaptor.forClass(TrainingSessionCompleted.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().elderId()).isEqualTo(elderId);
        assertThat(captor.getValue().sessionDate()).isEqualTo(today);
    }

    @Test
    void 본인_확인에_실패하면_발행하지_않는다() {
        given(careAccessQuery.elderIdForUser(elderUserId)).willReturn(elderId);
        willThrow(new DomainException(ErrorCode.CARE_ACCESS_DENIED))
                .given(careAccessQuery).requireSelf(elderUserId, elderId);

        assertThatThrownBy(() -> useCase.completeToday(elderUserId))
                .isInstanceOf(DomainException.class);

        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }
}
