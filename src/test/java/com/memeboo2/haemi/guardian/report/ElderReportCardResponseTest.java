package com.memeboo2.haemi.guardian.report;

import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.report.application.GetElderReportListUseCase.Card;
import com.memeboo2.haemi.guardian.report.domain.ReportStatus;
import com.memeboo2.haemi.guardian.report.presentation.dto.ElderReportCardResponse;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ElderReportCardResponseTest {

    @Test
    void from_mapsAllFields() {
        UUID elderId = UUID.randomUUID();
        Card card = new Card(elderId, "김할머니", GuardianRole.DAUGHTER, 82, true, ReportStatus.GOOD);

        ElderReportCardResponse response = ElderReportCardResponse.from(card);

        assertThat(response.elderId()).isEqualTo(elderId);
        assertThat(response.name()).isEqualTo("김할머니");
        assertThat(response.role()).isEqualTo(GuardianRole.DAUGHTER);
        assertThat(response.roleLabel()).isEqualTo("딸");
        assertThat(response.age()).isEqualTo(82);
        assertThat(response.attendedToday()).isTrue();
        assertThat(response.status()).isEqualTo(ReportStatus.GOOD);
    }
}
