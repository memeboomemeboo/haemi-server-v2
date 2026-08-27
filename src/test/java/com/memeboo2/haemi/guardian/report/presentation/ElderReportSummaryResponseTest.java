package com.memeboo2.haemi.guardian.report.presentation;

import com.memeboo2.haemi.guardian.report.application.GetElderReportSummaryUseCase.Summary;
import com.memeboo2.haemi.guardian.report.domain.ReportStatus;
import com.memeboo2.haemi.guardian.report.presentation.dto.ElderReportSummaryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ElderReportSummaryResponseTest {

    @Test
    @DisplayName("Summary로부터 리포트 요약 필드를 모두 매핑한다")
    void from_전체_필드를_매핑한다() {
        UUID elderId = UUID.randomUUID();
        Summary summary = new Summary(
                elderId, "김할머니", 82, "베이비붐 세대", 120L, true,
                5, 7, ReportStatus.GOOD, 4, 15);

        ElderReportSummaryResponse response = ElderReportSummaryResponse.from(summary);

        assertThat(response.elderId()).isEqualTo(elderId);
        assertThat(response.name()).isEqualTo("김할머니");
        assertThat(response.age()).isEqualTo(82);
        assertThat(response.generation()).isEqualTo("베이비붐 세대");
        assertThat(response.daysTogether()).isEqualTo(120L);
        assertThat(response.attendedToday()).isTrue();
        assertThat(response.weeklyParticipationDays()).isEqualTo(5);
        assertThat(response.weeklyGoalDays()).isEqualTo(7);
        assertThat(response.status()).isEqualTo(ReportStatus.GOOD);
        assertThat(response.currentStreak()).isEqualTo(4);
        assertThat(response.bestStreak()).isEqualTo(15);
    }

    @Test
    @DisplayName("나이가 없는 어르신은 age가 null이다")
    void from_나이가_없으면_age가_null이다() {
        Summary summary = new Summary(
                UUID.randomUUID(), "무연령어르신", null, null, 0L, false,
                0, 7, ReportStatus.WATCH, 0, 0);

        ElderReportSummaryResponse response = ElderReportSummaryResponse.from(summary);

        assertThat(response.age()).isNull();
        assertThat(response.generation()).isNull();
        assertThat(response.status()).isEqualTo(ReportStatus.WATCH);
    }
}
