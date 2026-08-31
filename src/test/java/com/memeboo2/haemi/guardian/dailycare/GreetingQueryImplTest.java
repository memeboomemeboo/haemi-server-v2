package com.memeboo2.haemi.guardian.dailycare;

import com.memeboo2.haemi.auth.api.AccountQuery;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.GreetingQuery;
import com.memeboo2.haemi.guardian.dailycare.application.GreetingQueryImpl;
import com.memeboo2.haemi.guardian.dailycare.domain.DailyCare;
import com.memeboo2.haemi.guardian.dailycare.infrastructure.DailyCareRepository;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GreetingQueryImplTest {

    @Mock DailyCareRepository dailyCareRepository;
    @Mock AccountQuery accountQuery;
    @Mock HaemiClock clock;
    @Mock MediaUploadCommand mediaUploadCommand;
    @InjectMocks GreetingQueryImpl greetingQuery;

    @Test
    void 발신_보호자_이름을_일괄_조회해_채운다() {
        UUID elderId = UUID.randomUUID();
        UUID guardianA = UUID.randomUUID();
        UUID guardianB = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 8, 25);

        DailyCare careA = DailyCare.text(guardianA, elderId, date, "안녕하세요", 30);
        DailyCare careB = DailyCare.text(guardianB, elderId, date, "잘 지내세요", 30);

        given(clock.now()).willReturn(Instant.parse("2026-08-25T00:00:00Z"));
        given(dailyCareRepository.findByElderIdAndDate(any(), any(), any()))
                .willReturn(List.of(careA, careB));
        given(accountQuery.findAllById(any())).willReturn(List.of(
                new AccountQuery.AccountInfo(guardianA, "딸 지영", "yjy", null, null, null, null),
                new AccountQuery.AccountInfo(guardianB, "아들 민수", "kms", null, null, null, null)
        ));

        List<GreetingQuery.ReceivedGreeting> result = greetingQuery.findFor(elderId, date);

        assertThat(result).extracting(GreetingQuery.ReceivedGreeting::guardianName)
                .containsExactly("딸 지영", "아들 민수");
    }

    @Test
    void 음성_인사는_현재_서빙_URL로_변환한다() {
        UUID elderId = UUID.randomUUID();
        UUID guardianId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 8, 25);
        DailyCare voice = DailyCare.voice(guardianId, elderId, date, "greeting_voice/confirmed.aac", 12, 30);
        given(clock.now()).willReturn(Instant.parse("2026-08-25T00:00:00Z"));
        given(dailyCareRepository.findByElderIdAndDate(any(), any(), any())).willReturn(List.of(voice));
        given(accountQuery.findAllById(any())).willReturn(List.of(
                new AccountQuery.AccountInfo(guardianId, "딸 지영", "yjy", null, null, null, null)));
        given(mediaUploadCommand.resolveServingUrl("greeting_voice/confirmed.aac"))
                .willReturn("https://cdn.example/greeting.aac");

        List<GreetingQuery.ReceivedGreeting> result = greetingQuery.findFor(elderId, date);

        assertThat(((GreetingQuery.GreetingContent.Voice) result.get(0).content()).mediaKey())
                .isEqualTo("https://cdn.example/greeting.aac");
        verify(mediaUploadCommand).resolveServingUrl("greeting_voice/confirmed.aac");
    }
}
