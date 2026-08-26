package com.memeboo2.haemi.guardian.report.presentation;

import com.memeboo2.haemi.guardian.report.application.GenerateElderReportPdfUseCase;
import com.memeboo2.haemi.guardian.report.application.ReportPeriod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Tag(name = "리포트 PDF (보호자)", description = "어르신 인지 회상 리포트 PDF 다운로드")
@RestController
@RequiredArgsConstructor
public class PdfReportController {

    private final GenerateElderReportPdfUseCase generatePdfUseCase;

    @Operation(summary = "어르신 인지 리포트 PDF 다운로드 (RPT-PDF-001)",
            description = "period=WEEKLY(최근 7일)·MONTHLY(최근 4주). 한글 임베드 PDF를 첨부로 반환한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "application/pdf 반환"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "인가 실패 — CARE_ACCESS_DENIED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "PDF 생성 실패 — REPORT_PDF_RENDER_FAILED")
    })
    @GetMapping("/api/v1/guardian/elders/{elderId}/report/pdf")
    public ResponseEntity<byte[]> download(
            @RequestAttribute UUID guardianId,
            @PathVariable UUID elderId,
            @RequestParam(defaultValue = "WEEKLY") ReportPeriod period) {

        GenerateElderReportPdfUseCase.Result result = generatePdfUseCase.execute(guardianId, elderId, period);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(result.filename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", disposition.toString())
                .body(result.pdf());
    }
}
