package com.memeboo2.haemi.guardian.report;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.report.application.UpdateWeeklyHighlightUseCase;
import com.memeboo2.haemi.guardian.report.application.WeeklyHighlightItem;
import com.memeboo2.haemi.guardian.report.domain.WeeklyHighlightOverride;
import com.memeboo2.haemi.guardian.report.infrastructure.WeeklyHighlightOverrideRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class UpdateWeeklyHighlightUseCaseTest {

    @Mock CareAccessQuery careAccessQuery;
    @Mock WeeklyHighlightOverrideRepository overrideRepository;
    @Mock HaemiClock clock;
    @InjectMocks UpdateWeeklyHighlightUseCase useCase;

    private final UUID guardianId = UUID.randomUUID();
    private final UUID elderId = UUID.randomUUID();

    @Test
    void 편집한_문구를_저장하고_반환한다() {
        given(clock.today()).willReturn(LocalDate.of(2026, 8, 27)); // 목요일 → 그 주 월요일 08-24
        given(overrideRepository.findByElderIdAndWeekStart(elderId, LocalDate.of(2026, 8, 24)))
                .willReturn(Optional.empty());
        given(overrideRepository.save(any(WeeklyHighlightOverride.class)))
                .willAnswer(inv -> inv.getArgument(0));

        UUID firstId = UUID.randomUUID();
        var result = useCase.executeItems(guardianId, elderId, List.of(
                new WeeklyHighlightItem(firstId, "참여", "이번 주 참 잘하셨어요"),
                new WeeklyHighlightItem(UUID.randomUUID(), "활동", "산책도 자주 하셨어요")));

        assertThat(result.elderId()).isEqualTo(elderId);
        assertThat(result.items()).extracting(WeeklyHighlightItem::id).containsExactly(firstId, result.items().get(1).id());
        assertThat(result.items()).extracting(WeeklyHighlightItem::body)
                .containsExactly("이번 주 참 잘하셨어요", "산책도 자주 하셨어요");
    }

    @Test
    void 링크없는_보호자는_403() {
        willThrow(new DomainException(ErrorCode.CARE_ACCESS_DENIED))
                .given(careAccessQuery).requireGuardianOf(guardianId, elderId);

        assertThatThrownBy(() -> useCase.executeItems(guardianId, elderId,
                List.of(new WeeklyHighlightItem(UUID.randomUUID(), "제목", "문구"))))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCode.CARE_ACCESS_DENIED);
    }

    @Test
    void 빈_문구는_400() {
        var result = List.of(
                new WeeklyHighlightItem(UUID.randomUUID(), " ", "본문"),
                new WeeklyHighlightItem(UUID.randomUUID(), "제목", ""));
        assertThatThrownBy(() -> useCase.executeItems(guardianId, elderId, result))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void 제어문자가_포함된_문구는_원인을_알려준다() {
        var items = List.of(new WeeklyHighlightItem(UUID.randomUUID(), "제목\u001f", "본문"));

        assertThatThrownBy(() -> useCase.executeItems(guardianId, elderId, items))
                .isInstanceOf(DomainException.class)
                .hasMessage("하이라이트 문구에는 제어문자를 포함할 수 없습니다.");
    }

    @Test
    void null_리스트는_400() {
        assertThatThrownBy(() -> useCase.executeItems(guardianId, elderId, null))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void 빈_리스트는_400() {
        assertThatThrownBy(() -> useCase.executeItems(guardianId, elderId, List.of()))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void 항목이_null이면_400() {
        var items = new java.util.ArrayList<WeeklyHighlightItem>();
        items.add(null); // item == null 분기
        assertThatThrownBy(() -> useCase.executeItems(guardianId, elderId, items))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void 제목이_null이면_400() {
        var items = new java.util.ArrayList<WeeklyHighlightItem>();
        items.add(new WeeklyHighlightItem(UUID.randomUUID(), null, "본문")); // title == null 분기
        assertThatThrownBy(() -> useCase.executeItems(guardianId, elderId, items))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void 본문이_null이면_400() {
        var items = new java.util.ArrayList<WeeklyHighlightItem>();
        items.add(new WeeklyHighlightItem(UUID.randomUUID(), "제목", null)); // body == null 분기
        assertThatThrownBy(() -> useCase.executeItems(guardianId, elderId, items))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void 본문에_제어문자가_있으면_400() {
        // body separator 분기 (title은 정상, body에 )
        var items = List.of(new WeeklyHighlightItem(UUID.randomUUID(), "제목", "본문끝"));
        assertThatThrownBy(() -> useCase.executeItems(guardianId, elderId, items))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void 항목_ID가_없으면_새로_발급한다() {
        given(clock.today()).willReturn(LocalDate.of(2026, 8, 27));
        given(overrideRepository.findByElderIdAndWeekStart(elderId, LocalDate.of(2026, 8, 24)))
                .willReturn(Optional.empty());
        given(overrideRepository.save(any(WeeklyHighlightOverride.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // id == null → UUID 자동 발급 분기
        var result = useCase.executeItems(guardianId, elderId, List.of(
                new WeeklyHighlightItem(null, "제목", "본문")));

        assertThat(result.items().get(0).id()).isNotNull();
    }

    @Test
    void 문구가_2000자를_넘으면_400() {
        // 길이 검증은 weekStart(clock.today()) 조회 전에 수행된다.
        String longBody = "가".repeat(2001);
        assertThatThrownBy(() -> useCase.executeItems(guardianId, elderId,
                List.of(new WeeklyHighlightItem(UUID.randomUUID(), "제목", longBody))))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void 기존_문구가_있으면_갱신한다() {
        given(clock.today()).willReturn(LocalDate.of(2026, 8, 27));
        WeeklyHighlightOverride existing = WeeklyHighlightOverride.of(
                elderId, LocalDate.of(2026, 8, 24), "예전");
        given(overrideRepository.findByElderIdAndWeekStart(elderId, LocalDate.of(2026, 8, 24)))
                .willReturn(Optional.of(existing)); // map(update) 분기

        var result = useCase.executeItems(guardianId, elderId, List.of(
                new WeeklyHighlightItem(UUID.randomUUID(), "제목", "새 본문")));

        assertThat(result.items().get(0).body()).isEqualTo("새 본문");
    }
}
