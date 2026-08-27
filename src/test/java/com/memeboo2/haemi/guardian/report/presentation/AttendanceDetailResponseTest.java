package com.memeboo2.haemi.guardian.report.presentation;

import com.memeboo2.haemi.guardian.report.application.GetAttendanceDetailUseCase.AttendanceDetail;
import com.memeboo2.haemi.guardian.report.application.GetAttendanceDetailUseCase.DayMark;
import com.memeboo2.haemi.guardian.report.application.GetAttendanceDetailUseCase.WeekBar;
import com.memeboo2.haemi.guardian.report.domain.ReportStatus;
import com.memeboo2.haemi.guardian.report.presentation.dto.AttendanceDetailResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AttendanceDetailResponseTest {

    @Test
    @DisplayName("AttendanceDetail로부터 최근 7일, 최근 4주, 연속 출석을 모두 매핑한다")
    void from_전체_필드를_매핑한다() {
        LocalDate day = LocalDate.of(2026, 8, 20);
        DayMark dayMark = new DayMark(day, DayOfWeek.THURSDAY, true, true, true, false, true);

        LocalDate weekStart = LocalDate.of(2026, 8, 17);
        LocalDate weekEnd = LocalDate.of(2026, 8, 23);
        WeekBar weekBar = new WeekBar(weekStart, weekEnd, 5);

        AttendanceDetail detail = new AttendanceDetail(
                List.of(dayMark), List.of(weekBar), 3, 10, ReportStatus.GOOD);

        AttendanceDetailResponse response = AttendanceDetailResponse.from(detail);

        assertThat(response.currentStreak()).isEqualTo(3);
        assertThat(response.bestStreak()).isEqualTo(10);
        assertThat(response.weeklyStatus()).isEqualTo(ReportStatus.GOOD);

        assertThat(response.last7Days()).hasSize(1);
        AttendanceDetailResponse.DayMarkResponse dayResponse = response.last7Days().get(0);
        assertThat(dayResponse.date()).isEqualTo(day);
        assertThat(dayResponse.dayOfWeek()).isEqualTo(DayOfWeek.THURSDAY);
        assertThat(dayResponse.participated()).isTrue();
        assertThat(dayResponse.training()).isTrue();
        assertThat(dayResponse.greetingRead()).isTrue();
        assertThat(dayResponse.memoryViewed()).isFalse();
        assertThat(dayResponse.replied()).isTrue();

        assertThat(response.last4Weeks()).hasSize(1);
        AttendanceDetailResponse.WeekBarResponse weekResponse = response.last4Weeks().get(0);
        assertThat(weekResponse.weekStart()).isEqualTo(weekStart);
        assertThat(weekResponse.weekEnd()).isEqualTo(weekEnd);
        assertThat(weekResponse.participatedDays()).isEqualTo(5);
    }

    @Test
    @DisplayName("불참한 날은 participated와 세부 활동이 모두 false다")
    void from_불참한_날은_모두_false다() {
        LocalDate day = LocalDate.of(2026, 8, 21);
        DayMark absent = new DayMark(day, DayOfWeek.FRIDAY, false, false, false, false, false);

        AttendanceDetail detail = new AttendanceDetail(
                List.of(absent), List.of(), 0, 5, ReportStatus.WATCH);

        AttendanceDetailResponse response = AttendanceDetailResponse.from(detail);

        AttendanceDetailResponse.DayMarkResponse dayResponse = response.last7Days().get(0);
        assertThat(dayResponse.participated()).isFalse();
        assertThat(dayResponse.training()).isFalse();
        assertThat(dayResponse.greetingRead()).isFalse();
        assertThat(dayResponse.memoryViewed()).isFalse();
        assertThat(dayResponse.replied()).isFalse();
        assertThat(response.last4Weeks()).isEmpty();
        assertThat(response.weeklyStatus()).isEqualTo(ReportStatus.WATCH);
    }
}
