package com.memeboo2.haemi.guardian.dailycare;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.event.GreetingSent;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.dailycare.application.DailyCareProperties;
import com.memeboo2.haemi.guardian.dailycare.application.DailyCareSaver;
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

    // 적재 본문은 REQUIRES_NEW 저장자에 있다. 유스케이스에는 실제 저장자를 넣어 경로 전체를 검증한다.
    @InjectMocks DailyCareSaver dailyCareSaver;

    SendDailyCareUseCase useCase;

    private static final UUID GUARDIAN = UUID.randomUUID();
    private static final UUID ELDER = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 23);

    @BeforeEach
    void setUp() {
        useCase = new SendDailyCareUseCase(careAccessQuery, props, clock, dailyCareSaver);
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
        assertThat(cap.getValue().careDate()).isEqualTo(TODAY);
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
    void 동시_전송으로_uk_daily_care_위반이_나면_409로_변환된다() {
        when(repo.existsByGuardianIdAndElderIdAndCareDate(GUARDIAN, ELDER, TODAY)).thenReturn(false);
        // 선검사를 통과한 뒤 flush에서 유니크 제약이 잡는 경쟁 상황.
        doThrow(new DataIntegrityViolationException(
                "ERROR: duplicate key value violates unique constraint \"uk_daily_care_guardian_elder_date\""))
                .when(repo).saveAndFlush(any(DailyCare.class));

        assertThatThrownBy(() -> useCase.sendText(GUARDIAN, ELDER, "안녕하세요"))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DAILY_CARE_ALREADY_SENT);

        // 위반 시 전송 이벤트를 발행하지 않는다.
        verify(publisher, never()).publishEvent(any(GreetingSent.class));
    }

    @Test
    void 다른_제약_위반은_409로_감추지_않고_그대로_전파한다() {
        when(repo.existsByGuardianIdAndElderIdAndCareDate(GUARDIAN, ELDER, TODAY)).thenReturn(false);
        doThrow(new DataIntegrityViolationException("null value in column \"elder_id\" violates not-null constraint"))
                .when(repo).saveAndFlush(any(DailyCare.class));

        assertThatThrownBy(() -> useCase.sendText(GUARDIAN, ELDER, "안녕하세요"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
