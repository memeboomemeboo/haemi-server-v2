package com.memeboo2.haemi.guardian.home;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.api.MemoryViewActivityQuery;
import com.memeboo2.haemi.guardian.api.ResponseQuery;
import com.memeboo2.haemi.guardian.api.TrainingActivityQuery;
import com.memeboo2.haemi.guardian.dailycare.domain.DailyCare;
import com.memeboo2.haemi.guardian.dailycare.infrastructure.DailyCareRepository;
import com.memeboo2.haemi.guardian.home.application.GetTodayActivitiesUseCase;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

/** GetTodayActivitiesUseCaseTest를 보완하는 추가 커버리지: 각 활동 종류·정렬·필터 분기. */
@ExtendWith(MockitoExtension.class)
class GetTodayActivitiesUseCaseAdditionalTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDate DATE = LocalDate.of(2026, 8, 27);

    @Mock CareAccessQuery careAccessQuery;
    @Mock TrainingActivityQuery trainingActivityQuery;
    @Mock ResponseQuery responseQuery;
    @Mock MemoryViewActivityQuery memoryViewActivityQuery;
    @Mock DailyCareRepository dailyCareRepository;
    @Mock MemoryRepository memoryRepository;
    @Mock HaemiClock clock;
    @InjectMocks GetTodayActivitiesUseCase useCase;

    private final UUID guardianId = UUID.randomUUID();
    private final UUID elderId = UUID.randomUUID();
    private final Instant from = DATE.atStartOfDay(KST).toInstant();
    private final Instant to = DATE.plusDays(1).atStartOfDay(KST).toInstant();

    @BeforeEach
    void setUp() {
        lenient().when(clock.now()).thenReturn(from.plusSeconds(3600));
        lenient().when(trainingActivityQuery.completedOn(elderId, DATE)).thenReturn(List.of());
        lenient().when(responseQuery.findByElderIdBetween(elderId, from, to)).thenReturn(List.of());
        lenient().when(dailyCareRepository.findByElderIdAndDate(eq(elderId), eq(DATE), any())).thenReturn(List.of());
        lenient().when(memoryViewActivityQuery.firstViewedBetween(eq(elderId), eq(from), eq(to))).thenReturn(List.of());
        lenient().when(memoryRepository.findAllById(any())).thenReturn(List.of());
    }

    @Test
    void 인지훈련_완료_활동이_포함된다() {
        Instant completedAt = from.plusSeconds(1000);
        given(trainingActivityQuery.completedOn(elderId, DATE))
                .willReturn(List.of(new TrainingActivityQuery.CompletedSession(completedAt)));

        List<GetTodayActivitiesUseCase.ActivityEntry> entries = useCase.execute(guardianId, elderId, DATE);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).type()).isEqualTo(GetTodayActivitiesUseCase.ActivityType.TRAINING_COMPLETED);
        assertThat(entries.get(0).title()).isEqualTo("인지 활동 완료");
    }

    @Test
    void 음성_답변은_응답유형과_추억ID를_상세에_담는다() {
        UUID memoryId = UUID.randomUUID();
        given(responseQuery.findByElderIdBetween(elderId, from, to)).willReturn(List.of(
                new ResponseQuery.ElderResponseActivity(memoryId, "VOICE", null, "안녕하세요", from.plusSeconds(10))));

        List<GetTodayActivitiesUseCase.ActivityEntry> entries = useCase.execute(guardianId, elderId, DATE);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).type()).isEqualTo(GetTodayActivitiesUseCase.ActivityType.RESPONSE_SENT);
        assertThat(entries.get(0).detail()).containsEntry("memoryId", memoryId)
                .containsEntry("responseType", "VOICE");
    }

    @Test
    void 텍스트_답변도_응답완료_타임라인으로_표시한다() {
        given(responseQuery.findByElderIdBetween(elderId, from, to)).willReturn(List.of(
                new ResponseQuery.ElderResponseActivity(UUID.randomUUID(), "TEXT", "고마워요", null, from.plusSeconds(10))));

        List<GetTodayActivitiesUseCase.ActivityEntry> entries = useCase.execute(guardianId, elderId, DATE);

        assertThat(entries.get(0).type()).isEqualTo(GetTodayActivitiesUseCase.ActivityType.RESPONSE_SENT);
        assertThat(entries.get(0).detail()).containsEntry("responseType", "TEXT");
    }

    @Test
    void 여러_답변은_각각_RESPONSE_SENT로_기록한다() {
        given(responseQuery.findByElderIdBetween(elderId, from, to)).willReturn(List.of(
                new ResponseQuery.ElderResponseActivity(UUID.randomUUID(), "EMOTION", null, null, from.plusSeconds(10)),
                new ResponseQuery.ElderResponseActivity(UUID.randomUUID(), "IMAGE", null, null, from.plusSeconds(20)),
                new ResponseQuery.ElderResponseActivity(UUID.randomUUID(), "UNKNOWN", null, null, from.plusSeconds(30))));

        List<GetTodayActivitiesUseCase.ActivityEntry> entries = useCase.execute(guardianId, elderId, DATE);

        assertThat(entries).extracting(GetTodayActivitiesUseCase.ActivityEntry::type)
                .containsOnly(GetTodayActivitiesUseCase.ActivityType.RESPONSE_SENT);
    }

    @Test
    void 열람된_하루한마디는_구간_내에서만_포함된다() {
        DailyCare viewedInRange = DailyCare.text(guardianId, elderId, DATE, "안부", 30);
        viewedInRange.markViewed(from.plusSeconds(500));
        DailyCare notViewed = DailyCare.text(guardianId, elderId, DATE, "안부2", 30);
        // viewedAt == null 이므로 필터에서 제외되어야 함

        given(dailyCareRepository.findByElderIdAndDate(eq(elderId), eq(DATE), any()))
                .willReturn(List.of(viewedInRange, notViewed));

        List<GetTodayActivitiesUseCase.ActivityEntry> entries = useCase.execute(guardianId, elderId, DATE);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).type()).isEqualTo(GetTodayActivitiesUseCase.ActivityType.GREETING_READ);
        assertThat(entries.get(0).title()).isEqualTo("하루 한마디 열람");
    }

    @Test
    void 모든_활동이_시각순으로_정렬된다() {
        given(trainingActivityQuery.completedOn(elderId, DATE))
                .willReturn(List.of(new TrainingActivityQuery.CompletedSession(from.plusSeconds(3000))));
        given(responseQuery.findByElderIdBetween(elderId, from, to)).willReturn(List.of(
                new ResponseQuery.ElderResponseActivity(UUID.randomUUID(), "TEXT", "hi", null, from.plusSeconds(100))));
        DailyCare viewed = DailyCare.text(guardianId, elderId, DATE, "안부", 30);
        viewed.markViewed(from.plusSeconds(2000));
        given(dailyCareRepository.findByElderIdAndDate(eq(elderId), eq(DATE), any())).willReturn(List.of(viewed));

        List<GetTodayActivitiesUseCase.ActivityEntry> entries = useCase.execute(guardianId, elderId, DATE);

        assertThat(entries).extracting(GetTodayActivitiesUseCase.ActivityEntry::type)
                .containsExactly(
                        GetTodayActivitiesUseCase.ActivityType.RESPONSE_SENT,
                        GetTodayActivitiesUseCase.ActivityType.GREETING_READ,
                        GetTodayActivitiesUseCase.ActivityType.TRAINING_COMPLETED);
    }

    @Test
    void executeToday는_clock의_오늘_날짜를_사용한다() {
        given(clock.today()).willReturn(DATE);
        given(trainingActivityQuery.completedOn(elderId, DATE)).willReturn(List.of());
        given(responseQuery.findByElderIdBetween(elderId, from, to)).willReturn(List.of());
        given(dailyCareRepository.findByElderIdAndDate(eq(elderId), eq(DATE), any())).willReturn(List.of());

        List<GetTodayActivitiesUseCase.ActivityEntry> entries = useCase.executeToday(guardianId, elderId);

        assertThat(entries).isEmpty();
        verify(careAccessQuery).requireGuardianOf(guardianId, elderId);
    }
}
