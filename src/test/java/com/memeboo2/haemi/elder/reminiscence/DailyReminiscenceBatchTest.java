package com.memeboo2.haemi.elder.reminiscence;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.elder.reminiscence.application.DailyReminiscenceBatch;
import com.memeboo2.haemi.elder.reminiscence.application.ReminiscenceService;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class DailyReminiscenceBatchTest {

    @Mock CareAccessQuery careAccessQuery;
    @Mock ReminiscenceService reminiscenceService;
    @Mock HaemiClock clock;
    @InjectMocks DailyReminiscenceBatch batch;

    UUID guardianId = UUID.randomUUID();
    UUID elderId1 = UUID.randomUUID();
    UUID elderId2 = UUID.randomUUID();

    @Test
    void generateForAll_고유_어르신_모두_처리() {
        LocalDate date = LocalDate.of(2026, 8, 25);
        CareAccessQuery.CareLink link1 = new CareAccessQuery.CareLink(guardianId, elderId1, GuardianRole.GUARDIAN);
        CareAccessQuery.CareLink link2 = new CareAccessQuery.CareLink(guardianId, elderId1, GuardianRole.DAUGHTER);
        CareAccessQuery.CareLink link3 = new CareAccessQuery.CareLink(guardianId, elderId2, GuardianRole.GUARDIAN);
        given(careAccessQuery.allLinks()).willReturn(List.of(link1, link2, link3));

        DailyReminiscenceBatch.BatchResult result = batch.generateForAll(date);

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.succeeded()).isEqualTo(2);
        assertThat(result.failed()).isEqualTo(0);
        then(reminiscenceService).should().generateForElder(elderId1, date);
        then(reminiscenceService).should().generateForElder(elderId2, date);
    }

    @Test
    void generateForAll_성공_실패_카운트() {
        LocalDate date = LocalDate.of(2026, 8, 25);
        CareAccessQuery.CareLink link1 = new CareAccessQuery.CareLink(guardianId, elderId1, GuardianRole.GUARDIAN);
        CareAccessQuery.CareLink link2 = new CareAccessQuery.CareLink(guardianId, elderId2, GuardianRole.GUARDIAN);
        given(careAccessQuery.allLinks()).willReturn(List.of(link1, link2));
        org.mockito.Mockito.lenient().doThrow(new RuntimeException("생성 실패"))
                .when(reminiscenceService).generateForElder(elderId1, date);

        DailyReminiscenceBatch.BatchResult result = batch.generateForAll(date);

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.succeeded()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
    }
}
