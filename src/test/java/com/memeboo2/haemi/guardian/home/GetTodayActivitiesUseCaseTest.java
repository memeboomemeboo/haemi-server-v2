package com.memeboo2.haemi.guardian.home;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.api.MemoryViewActivityQuery;
import com.memeboo2.haemi.guardian.api.ResponseQuery;
import com.memeboo2.haemi.guardian.api.TrainingActivityQuery;
import com.memeboo2.haemi.guardian.dailycare.infrastructure.DailyCareRepository;
import com.memeboo2.haemi.guardian.home.application.GetTodayActivitiesUseCase;
import com.memeboo2.haemi.guardian.home.application.GetTodayActivitiesUseCase.ActivityType;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class GetTodayActivitiesUseCaseTest {

    @Mock CareAccessQuery careAccessQuery;
    @Mock TrainingActivityQuery trainingActivityQuery;
    @Mock ResponseQuery responseQuery;
    @Mock MemoryViewActivityQuery memoryViewActivityQuery;
    @Mock DailyCareRepository dailyCareRepository;
    @Mock MemoryRepository memoryRepository;
    @Mock HaemiClock clock;
    @InjectMocks GetTodayActivitiesUseCase useCase;

    private final UUID guardianId = UUID.randomUUID();
    private final UUID elderId = UUID.randomUUID();
    private final LocalDate today = LocalDate.of(2026, 8, 27);

    @Test
    void 인지훈련과_추억답변을_시각순으로_병합한다() {
        given(clock.now()).willReturn(Instant.parse("2026-08-27T05:00:00Z"));
        given(clock.today()).willReturn(today);
        Instant training = Instant.parse("2026-08-27T00:20:00Z"); // 09:20 KST
        Instant response = Instant.parse("2026-08-27T02:05:00Z"); // 11:05 KST
        given(trainingActivityQuery.completedOn(elderId, today))
                .willReturn(List.of(new TrainingActivityQuery.CompletedSession(training)));
        given(responseQuery.findByElderIdBetween(any(), any(), any()))
                .willReturn(List.of(new ResponseQuery.ElderResponseActivity(
                        UUID.randomUUID(), "VOICE", null, "밥 잘 먹었다", response)));
        given(dailyCareRepository.findByElderIdAndDate(any(), any(), any())).willReturn(List.of());
        given(memoryViewActivityQuery.firstViewedBetween(any(), any(), any())).willReturn(List.of());
        given(memoryRepository.findAllById(any())).willReturn(List.of());

        var entries = useCase.executeToday(guardianId, elderId);

        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).type()).isEqualTo(ActivityType.TRAINING_COMPLETED);
        assertThat(entries.get(1).type()).isEqualTo(ActivityType.RESPONSE_SENT);
        assertThat(entries.get(1).detail()).containsEntry("responseType", "VOICE");
    }

    @Test
    void 링크없는_보호자는_403() {
        willThrow(new DomainException(ErrorCode.CARE_ACCESS_DENIED))
                .given(careAccessQuery).requireGuardianOf(guardianId, elderId);

        assertThatThrownBy(() -> useCase.execute(guardianId, elderId, today))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCode.CARE_ACCESS_DENIED);
    }
}
