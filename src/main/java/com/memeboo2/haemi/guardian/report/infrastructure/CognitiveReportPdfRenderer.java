package com.memeboo2.haemi.guardian.report.infrastructure;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.report.application.GetAttendanceDetailUseCase.AttendanceDetail;
import com.memeboo2.haemi.guardian.report.application.GetAttendanceDetailUseCase.DayMark;
import com.memeboo2.haemi.guardian.report.application.GetAttendanceDetailUseCase.WeekBar;
import com.memeboo2.haemi.guardian.report.application.GetElderReportSummaryUseCase.Summary;
import com.memeboo2.haemi.guardian.report.application.ReportPdfPort;
import com.memeboo2.haemi.guardian.report.domain.ReportStatus;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;

/** openhtmltopdf 기반 한글 임베드 PDF 렌더러. NanumGothic TTF를 클래스패스에서 등록한다. */
@Component
public class CognitiveReportPdfRenderer implements ReportPdfPort {

    private static final String FONT_CLASSPATH = "/fonts/NanumGothic-Regular.ttf";
    private static final String FONT_FAMILY = "NanumGothic";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter MD = DateTimeFormatter.ofPattern("M/d");
    private static final String[] DOW_KR = {"월", "화", "수", "목", "금", "토", "일"};

    @Override
    public byte[] render(ReportView view) {
        String html = buildHtml(view);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFont(() -> fontStream(), FONT_FAMILY);
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new DomainException(ErrorCode.REPORT_PDF_RENDER_FAILED, "리포트 PDF 생성에 실패했습니다.");
        }
    }

    private InputStream fontStream() {
        InputStream in = getClass().getResourceAsStream(FONT_CLASSPATH);
        if (in == null) {
            throw new IllegalStateException("리포트 폰트 리소스를 찾을 수 없습니다: " + FONT_CLASSPATH);
        }
        return in;
    }

    private String buildHtml(ReportView view) {
        Summary s = view.summary();
        AttendanceDetail a = view.attendance();

        StringBuilder days = new StringBuilder();
        for (DayMark d : a.last7Days()) {
            String dow = DOW_KR[d.dayOfWeek().getValue() - 1];
            String mark = d.participated() ? "●" : "○";
            String color = d.participated() ? "#2e7d32" : "#bdbdbd";
            days.append("<td class=\"day\"><div class=\"dow\">").append(dow).append("</div>")
                    .append("<div class=\"dot\" style=\"color:").append(color).append("\">").append(mark).append("</div>")
                    .append("<div class=\"date\">").append(esc(d.date().format(MD))).append("</div></td>");
        }

        StringBuilder weeks = new StringBuilder();
        int maxDays = view.attendance().last4Weeks().stream().mapToInt(WeekBar::participatedDays).max().orElse(0);
        maxDays = Math.max(maxDays, 1);
        for (WeekBar w : a.last4Weeks()) {
            int heightPct = (int) Math.round(100.0 * w.participatedDays() / maxDays);
            weeks.append("<td class=\"wcell\"><div class=\"barwrap\"><div class=\"bar\" style=\"height:")
                    .append(Math.max(heightPct, 4)).append("px\"></div></div>")
                    .append("<div class=\"wlabel\">").append(esc(w.weekStart().format(MD))).append("~")
                    .append(esc(w.weekEnd().format(MD))).append("</div>")
                    .append("<div class=\"wcount\">").append(w.participatedDays()).append("일</div></td>");
        }

        String statusLabel = statusLabel(s.status());
        String statusColor = statusColor(s.status());
        String ageText = s.age() == null ? "-" : s.age() + "세";
        String generation = s.generation() == null ? "" : " · " + esc(s.generation());

        return """
                <!DOCTYPE html>
                <html><head><meta charset="utf-8"/>
                <style>
                  @page { size: A4; margin: 22mm 18mm; }
                  body { font-family: 'NanumGothic', sans-serif; color: #212121; font-size: 11pt; }
                  h1 { font-size: 20pt; margin: 0 0 2mm 0; }
                  .subtitle { color: #616161; font-size: 10pt; margin-bottom: 8mm; }
                  .card { border: 1px solid #e0e0e0; border-radius: 6px; padding: 6mm; margin-bottom: 6mm; }
                  .name { font-size: 15pt; font-weight: bold; }
                  .meta { color: #616161; font-size: 10pt; margin-top: 1mm; }
                  .status { display: inline-block; padding: 1mm 3mm; border-radius: 4px; color: #fff; font-weight: bold; }
                  table.grid { width: 100%%; border-collapse: collapse; margin-top: 3mm; }
                  td.day, td.wcell { text-align: center; vertical-align: bottom; padding: 1mm; }
                  .dow { color: #616161; font-size: 9pt; }
                  .dot { font-size: 16pt; }
                  .date { color: #9e9e9e; font-size: 8pt; }
                  .barwrap { height: 100px; display: flex; align-items: flex-end; justify-content: center; }
                  .bar { width: 10mm; background: #4e7cff; border-radius: 3px 3px 0 0; }
                  .wlabel { color: #616161; font-size: 8pt; margin-top: 1mm; }
                  .wcount { font-weight: bold; font-size: 9pt; }
                  .section-title { font-weight: bold; font-size: 12pt; margin: 6mm 0 2mm 0; }
                  .kv { margin: 1mm 0; }
                  .footer { color: #9e9e9e; font-size: 8pt; margin-top: 10mm; }
                </style></head>
                <body>
                  <h1>%s 인지 회상 리포트</h1>
                  <div class="subtitle">생성일 %s · 보호자 관계: %s</div>

                  <div class="card">
                    <div class="name">%s</div>
                    <div class="meta">%s%s · 함께한 지 %d일</div>
                    <div style="margin-top:3mm">종합 상태 <span class="status" style="background:%s">%s</span></div>
                    <div class="kv">이번 주 참여: %d / %d일</div>
                    <div class="kv">연속 참여: 현재 %d일 · 최고 %d일</div>
                    <div class="kv">오늘 참여: %s</div>
                  </div>

                  <div class="section-title">최근 7일 출석</div>
                  <table class="grid"><tr>%s</tr></table>

                  <div class="section-title">최근 4주 참여</div>
                  <table class="grid"><tr>%s</tr></table>

                  <div class="footer">본 리포트는 어르신 간 비교·순위·수치 점수를 제공하지 않습니다. 참여 현황 관찰용입니다.</div>
                </body></html>
                """.formatted(
                view.period().label(),
                esc(view.generatedOn().format(DATE)),
                esc(view.guardianRoleLabel()),
                esc(s.name()),
                ageText, generation, s.daysTogether(),
                statusColor, statusLabel,
                s.weeklyParticipationDays(), s.weeklyGoalDays(),
                s.currentStreak(), s.bestStreak(),
                s.attendedToday() ? "예" : "아니오",
                days, weeks);
    }

    private String statusLabel(ReportStatus status) {
        return switch (status) {
            case WATCH -> "관찰 필요";
            case NORMAL -> "보통";
            case GOOD -> "좋음";
        };
    }

    private String statusColor(ReportStatus status) {
        return switch (status) {
            case WATCH -> "#ef8a3a";
            case NORMAL -> "#f2c200";
            case GOOD -> "#2e7d32";
        };
    }

    private String esc(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
