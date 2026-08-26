package com.memeboo2.haemi.guardian.report.application;

import com.memeboo2.haemi.auth.api.AccountQuery;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/** 정기 리포트 발송: 전체 보호자-어르신 링크를 순회하며 PDF를 이메일로 보낸다. */
@Service
@RequiredArgsConstructor
public class ReportDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(ReportDeliveryService.class);

    private final CareAccessQuery careAccessQuery;
    private final AccountQuery accountQuery;
    private final GenerateElderReportPdfUseCase generatePdfUseCase;
    private final ReportMailPort mailPort;

    /** 지정 주기로 전체 링크에 리포트를 발송하고, 성공 건수를 반환한다. */
    public DispatchResult dispatchAll(ReportPeriod period) {
        int sent = 0;
        int skipped = 0;
        int failed = 0;

        for (CareAccessQuery.CareLink link : careAccessQuery.allLinks()) {
            Optional<String> email = accountQuery.emailOf(link.guardianId());
            if (email.isEmpty()) {
                skipped++;
                continue;
            }
            try {
                GenerateElderReportPdfUseCase.Result pdf =
                        generatePdfUseCase.execute(link.guardianId(), link.elderId(), period);
                String subject = "[해미] %s 인지 회상 리포트".formatted(period.label());
                String body = "%s 인지 회상 리포트를 첨부합니다.\n해미와 함께해 주셔서 감사합니다."
                        .formatted(period.label());
                mailPort.sendReport(email.get(), subject, body, pdf.filename(), pdf.pdf());
                sent++;
            } catch (RuntimeException e) {
                failed++;
                log.warn("리포트 발송 실패: guardianId={}, elderId={}, period={}, cause={}",
                        link.guardianId(), link.elderId(), period, e.toString());
            }
        }

        log.info("리포트 정기 발송 완료: period={}, sent={}, skipped(email없음)={}, failed={}",
                period, sent, skipped, failed);
        return new DispatchResult(period, sent, skipped, failed);
    }

    public record DispatchResult(ReportPeriod period, int sent, int skipped, int failed) {}

    /** 단건 발송 (테스트·재발송용). */
    public boolean dispatchOne(UUID guardianId, UUID elderId, ReportPeriod period) {
        Optional<String> email = accountQuery.emailOf(guardianId);
        if (email.isEmpty()) {
            return false;
        }
        GenerateElderReportPdfUseCase.Result pdf = generatePdfUseCase.execute(guardianId, elderId, period);
        String subject = "[해미] %s 인지 회상 리포트".formatted(period.label());
        String body = "%s 인지 회상 리포트를 첨부합니다.".formatted(period.label());
        mailPort.sendReport(email.get(), subject, body, pdf.filename(), pdf.pdf());
        return true;
    }
}
