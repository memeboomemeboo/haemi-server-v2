package com.memeboo2.haemi.guardian.dailycare;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.event.GreetingSent;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.dailycare.application.DailyCareProperties;
import com.memeboo2.haemi.guardian.dailycare.application.SendDailyCareUseCase;
import com.memeboo2.haemi.guardian.dailycare.domain.DailyCare;
import com.memeboo2.haemi.guardian.dailycare.infrastructure.DailyCareRepository;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendDailyCareUseCaseTest {

    @Mock DailyCareRepository repo;
    @Mock CareAccessQuery careAccessQuery;
    @Mock DailyCareProperties props;
    @Mock HaemiClock clock;
    @Mock ApplicationEventPublisher publisher;
    @Mock MediaUploadCommand mediaUploadCommand;

    @InjectMocks SendDailyCareUseCase useCase;

    private static final UUID GUARDIAN = UUID.randomUUID();
    private static final UUID ELDER = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 23);

    @BeforeEach
    void setUp() {
        lenient().when(clock.today()).thenReturn(TODAY);
        lenient().when(clock.now()).thenReturn(Instant.now());
        lenient().when(props.retentionDays()).thenReturn(30);
        lenient().when(props.maxVoiceDurationSeconds()).thenReturn(60);
        lenient().when(repo.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void 텍스트_하루한마디_정상() {
        when(repo.existsByGuardianIdAndElderIdAndCareDate(GUARDIAN, ELDER, TODAY)).thenReturn(false);

        useCase.sendText(GUARDIAN, ELDER, "오늘도 건강하세요!");

        verify(repo).saveAndFlush(any(DailyCare.class));
        ArgumentCaptor<GreetingSent> cap = ArgumentCaptor.forClass(GreetingSent.class);
        verify(publisher).publishEvent(cap.capture());
        assertThat(cap.getValue().elderId()).isEqualTo(ELDER);
    }

    @Test
    void 링크없는_보호자는_403() {
        doThrow(new DomainException(ErrorCode.CARE_ACCESS_DENIED, ""))
                .when(careAccessQuery).requireGuardianOf(GUARDIAN, ELDER);

        assertThatThrownBy(() -> useCase.sendText(GUARDIAN, ELDER, "안녕하세요"))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARE_ACCESS_DENIED);
    }

    @Test
    void 오늘_이미_보냈으면_409() {
        when(repo.existsByGuardianIdAndElderIdAndCareDate(GUARDIAN, ELDER, TODAY)).thenReturn(true);

        assertThatThrownBy(() -> useCase.sendText(GUARDIAN, ELDER, "또 보내요"))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DAILY_CARE_ALREADY_SENT);
    }

    @Test
    void 텍스트_100자_초과_400() {
        when(repo.existsByGuardianIdAndElderIdAndCareDate(GUARDIAN, ELDER, TODAY)).thenReturn(false);

        String longText = "가".repeat(101);
        assertThatThrownBy(() -> useCase.sendText(GUARDIAN, ELDER, longText))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void 음성_60초_초과_400() {
        assertThatThrownBy(() -> useCase.sendVoice(GUARDIAN, ELDER, UUID.randomUUID(), 61))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void 동시_전송_유니크_충돌은_409로_변환한다() {
        when(repo.existsByGuardianIdAndElderIdAndCareDate(GUARDIAN, ELDER, TODAY)).thenReturn(false);
        doThrow(new DataIntegrityViolationException("uk_daily_cares"))
                .when(repo).saveAndFlush(any(DailyCare.class));

        assertThatThrownBy(() -> useCase.sendText(GUARDIAN, ELDER, "안녕하세요"))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DAILY_CARE_ALREADY_SENT);
    }
}
