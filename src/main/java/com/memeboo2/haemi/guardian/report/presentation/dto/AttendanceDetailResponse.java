package com.memeboo2.haemi.guardian.report.presentation.dto;

import com.memeboo2.haemi.guardian.report.application.GetAttendanceDetailUseCase.AttendanceDetail;
import com.memeboo2.haemi.guardian.report.application.GetAttendanceDetailUseCase.DayMark;
import com.memeboo2.haemi.guardian.report.application.GetAttendanceDetailUseCase.WeekBar;
import com.memeboo2.haemi.guardian.report.domain.ReportStatus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public record AttendanceDetailResponse(
        List<DayMarkResponse> last7Days,
        List<WeekBarResponse> last4Weeks,
        int currentStreak,
        int bestStreak,
        ReportStatus weeklyStatus
) {
    public record DayMarkResponse(LocalDate date, DayOfWeek dayOfWeek, boolean participated,
                                  boolean training, boolean greetingRead, boolean memoryViewed, boolean replied) {
        static DayMarkResponse from(DayMark m) {
            return new DayMarkResponse(m.date(), m.dayOfWeek(), m.participated(),
                    m.training(), m.greetingRead(), m.memoryViewed(), m.replied());
        }
    }

    public record WeekBarResponse(LocalDate weekStart, LocalDate weekEnd, int participatedDays) {
        static WeekBarResponse from(WeekBar w) {
            return new WeekBarResponse(w.weekStart(), w.weekEnd(), w.participatedDays());
        }
    }

    public static AttendanceDetailResponse from(AttendanceDetail d) {
        return new AttendanceDetailResponse(
                d.last7Days().stream().map(DayMarkResponse::from).toList(),
                d.last4Weeks().stream().map(WeekBarResponse::from).toList(),
                d.currentStreak(), d.bestStreak(), d.weeklyStatus()
        );
    }
}
