package com.memeboo2.haemi.guardian.home;

import com.memeboo2.haemi.auth.api.AccountQuery;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.AttendanceQuery;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.dailycare.infrastructure.DailyCareRepository;
import com.memeboo2.haemi.guardian.eldermanagement.domain.Elder;
import com.memeboo2.haemi.guardian.eldermanagement.domain.ElderRepository;
import com.memeboo2.haemi.guardian.home.application.GetGuardianHomeUseCase;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
import com.memeboo2.haemi.guardian.report.api.CognitiveStatusQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class GetGuardianHomeUseCaseTest {

    @Mock CareAccessQuery careAccessQuery;
    @Mock ElderRepository elderRepository;
    @Mock DailyCareRepository dailyCareRepository;
    @Mock MemoryRepository memoryRepository;
    @Mock AttendanceQuery attendanceQuery;
    @Mock AccountQuery accountQuery;
    @Mock CognitiveStatusQuery cognitiveStatusQuery;
    @Mock HaemiClock clock;
    @InjectMocks GetGuardianHomeUseCase useCase;

    @Test
    void 어르신_카드에_마지막_접속_시각을_포함한다() {
        UUID guardianId = UUID.randomUUID();
        UUID elderId = UUID.randomUUID();
        UUID elderUserId = UUID.randomUUID();
        Instant lastLoginAt = Instant.parse("2026-08-24T09:00:00Z");
        LocalDate today = LocalDate.of(2026, 8, 25);

        given(careAccessQuery.accessibleElders(guardianId)).willReturn(List.of(elderId));
        given(clock.today()).willReturn(today);
        Elder elder = Elder.create(elderUserId, UUID.randomUUID(), "황정빈", LocalDate.of(1950, 1, 1));
        given(elderRepository.findById(elderId)).willReturn(Optional.of(elder));
        given(careAccessQuery.roleOf(guardianId, elderId)).willReturn(GuardianRole.DAUGHTER);
        given(dailyCareRepository.existsByGuardianIdAndElderIdAndCareDate(guardianId, elderId, today))
                .willReturn(false);
        given(accountQuery.findById(elderUserId)).willReturn(Optional.of(
                new AccountQuery.AccountInfo(elderUserId, "황정빈", "elder01", "010", null, null, lastLoginAt)));
        lenient().when(memoryRepository.existsByCreatedByAndCreatedAtAfter(any(), any())).thenReturn(false);
        lenient().when(cognitiveStatusQuery.cognitiveStatus(any(), any())).thenReturn(
                new CognitiveStatusQuery.CognitiveStatusView(elderId, List.of()));

        var result = useCase.execute(guardianId);

        assertThat(result.elders()).hasSize(1);
        assertThat(result.elders().get(0).lastLoginAt()).isEqualTo(lastLoginAt);
    }

    @Test
    void 접속_기록이_없으면_null이다() {
        UUID guardianId = UUID.randomUUID();
        UUID elderId = UUID.randomUUID();
        UUID elderUserId = UUID.randomUUID();
        LocalDate today = LocalDate.of(2026, 8, 25);

        given(careAccessQuery.accessibleElders(guardianId)).willReturn(List.of(elderId));
        given(clock.today()).willReturn(today);
        Elder elder = Elder.create(elderUserId, UUID.randomUUID(), "황정빈", LocalDate.of(1950, 1, 1));
        given(elderRepository.findById(elderId)).willReturn(Optional.of(elder));
        given(careAccessQuery.roleOf(guardianId, elderId)).willReturn(GuardianRole.DAUGHTER);
        given(dailyCareRepository.existsByGuardianIdAndElderIdAndCareDate(guardianId, elderId, today))
                .willReturn(false);
        given(accountQuery.findById(elderUserId)).willReturn(Optional.empty());
        lenient().when(memoryRepository.existsByCreatedByAndCreatedAtAfter(any(), any())).thenReturn(false);
        lenient().when(cognitiveStatusQuery.cognitiveStatus(any(), any())).thenReturn(
                new CognitiveStatusQuery.CognitiveStatusView(elderId, List.of()));

        var result = useCase.execute(guardianId);

        assertThat(result.elders().get(0).lastLoginAt()).isNull();
    }
}
