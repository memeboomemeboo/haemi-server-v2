package com.memeboo2.haemi.guardian.report;

import com.memeboo2.haemi.auth.api.AccountQuery;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.report.application.GenerateElderReportPdfUseCase;
import com.memeboo2.haemi.guardian.report.application.ReportDeliveryService;
import com.memeboo2.haemi.guardian.report.application.ReportMailPort;
import com.memeboo2.haemi.guardian.report.application.ReportPeriod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class ReportDeliveryServiceTest {

    @Mock CareAccessQuery careAccessQuery;
    @Mock AccountQuery accountQuery;
    @Mock GenerateElderReportPdfUseCase generatePdfUseCase;
    @Mock ReportMailPort mailPort;
    @InjectMocks ReportDeliveryService service;

    UUID guardianId1 = UUID.randomUUID();
    UUID elderId1 = UUID.randomUUID();
    UUID guardianId2 = UUID.randomUUID();
    UUID elderId2 = UUID.randomUUID();

    @Test
    void dispatchAll_이메일있는_모든_링크에_발송() {
        CareAccessQuery.CareLink link = new CareAccessQuery.CareLink(guardianId1, elderId1, GuardianRole.GUARDIAN);
        given(careAccessQuery.allLinks()).willReturn(List.of(link));
        given(accountQuery.emailOf(guardianId1)).willReturn(Optional.of("hjbin1211@gmail.com"));
        GenerateElderReportPdfUseCase.Result pdfResult =
                new GenerateElderReportPdfUseCase.Result("report.pdf", new byte[]{1, 2, 3});
        given(generatePdfUseCase.execute(guardianId1, elderId1, ReportPeriod.WEEKLY)).willReturn(pdfResult);

        ReportDeliveryService.DispatchResult result = service.dispatchAll(ReportPeriod.WEEKLY);

        assertThat(result.sent()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(0);
        assertThat(result.failed()).isEqualTo(0);
        then(mailPort).should().sendReport(anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void dispatchAll_이메일없는_링크는_스킵() {
        CareAccessQuery.CareLink link = new CareAccessQuery.CareLink(guardianId1, elderId1, GuardianRole.GUARDIAN);
        given(careAccessQuery.allLinks()).willReturn(List.of(link));
        given(accountQuery.emailOf(guardianId1)).willReturn(Optional.empty());

        ReportDeliveryService.DispatchResult result = service.dispatchAll(ReportPeriod.WEEKLY);

        assertThat(result.sent()).isEqualTo(0);
        assertThat(result.skipped()).isEqualTo(1);
        then(generatePdfUseCase).should(org.mockito.Mockito.never()).execute(any(), any(), any());
        then(mailPort).should(org.mockito.Mockito.never())
                .sendReport(anyString(), anyString(), anyString(), anyString(), any());
        // note: byte[] pdf param matched via any() above
    }

    @Test
    void dispatchAll_실패건_카운트() {
        CareAccessQuery.CareLink link1 = new CareAccessQuery.CareLink(guardianId1, elderId1, GuardianRole.GUARDIAN);
        CareAccessQuery.CareLink link2 = new CareAccessQuery.CareLink(guardianId2, elderId2, GuardianRole.GUARDIAN);
        given(careAccessQuery.allLinks()).willReturn(List.of(link1, link2));
        given(accountQuery.emailOf(guardianId1)).willReturn(Optional.of("a@example.com"));
        given(accountQuery.emailOf(guardianId2)).willReturn(Optional.of("b@example.com"));
        given(generatePdfUseCase.execute(guardianId1, elderId1, ReportPeriod.WEEKLY))
                .willThrow(new RuntimeException("pdf 생성 실패"));
        GenerateElderReportPdfUseCase.Result pdfResult =
                new GenerateElderReportPdfUseCase.Result("report.pdf", new byte[]{1});
        given(generatePdfUseCase.execute(guardianId2, elderId2, ReportPeriod.WEEKLY)).willReturn(pdfResult);

        ReportDeliveryService.DispatchResult result = service.dispatchAll(ReportPeriod.WEEKLY);

        assertThat(result.sent()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(0);
    }

    @Test
    void dispatchOne_이메일없으면_false() {
        given(accountQuery.emailOf(guardianId1)).willReturn(Optional.empty());

        boolean result = service.dispatchOne(guardianId1, elderId1, ReportPeriod.MONTHLY);

        assertThat(result).isFalse();
        then(generatePdfUseCase).should(org.mockito.Mockito.never()).execute(any(), any(), any());
    }
}
