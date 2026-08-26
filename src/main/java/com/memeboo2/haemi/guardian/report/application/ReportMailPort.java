package com.memeboo2.haemi.guardian.report.application;

/** 인지 리포트 PDF를 이메일 첨부로 발송한다. SMTP 미설정 환경은 로깅 대체 구현이 등록된다. */
public interface ReportMailPort {

    void sendReport(String toEmail, String subject, String bodyText, String attachmentFilename, byte[] pdf);
}
