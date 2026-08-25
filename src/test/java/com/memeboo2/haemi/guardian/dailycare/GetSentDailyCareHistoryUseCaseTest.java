package com.memeboo2.haemi.guardian.dailycare;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.dailycare.application.GetSentDailyCareHistoryUseCase;
import com.memeboo2.haemi.guardian.dailycare.domain.DailyCare;
import com.memeboo2.haemi.guardian.dailycare.infrastructure.DailyCareRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class GetSentDailyCareHistoryUseCaseTest {

    @Mock CareAccessQuery careAccessQuery;
    @Mock DailyCareRepository dailyCareRepository;
    @InjectMocks GetSentDailyCareHistoryUseCase useCase;

    UUID guardianId = UUID.randomUUID();
    UUID elderId = UUID.randomUUID();

    @Test
    void 정상_경로() {
        DailyCare care = DailyCare.text(guardianId, elderId, LocalDate.of(2026, 8, 20), "안녕하세요", 30);
        given(dailyCareRepository.findByGuardianIdAndElderIdOrderByCareDateDescCreatedAtDesc(guardianId, elderId))
                .willReturn(List.of(care));

        List<DailyCare> result = useCase.execute(guardianId, elderId);

        assertThat(result).containsExactly(care);
    }

    @Test
    void 링크없는_보호자는_403() {
        willThrow(new DomainException(ErrorCode.CARE_ACCESS_DENIED))
                .given(careAccessQuery).requireGuardianOf(guardianId, elderId);

        assertThatThrownBy(() -> useCase.execute(guardianId, elderId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CARE_ACCESS_DENIED));
    }
}
