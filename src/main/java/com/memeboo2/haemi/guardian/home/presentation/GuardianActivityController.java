package com.memeboo2.haemi.guardian.home.presentation;

import com.memeboo2.haemi.common.web.ApiResponse;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.home.application.GetTodayActivitiesUseCase;
import com.memeboo2.haemi.guardian.home.presentation.dto.TodayActivitiesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Tag(name = "오늘의 기록 (보호자)", description = "보호자 홈 어르신 활동 타임라인")
@RestController
@RequestMapping("/api/v1/guardian/elders/{elderId}/activities")
@RequiredArgsConstructor
public class GuardianActivityController {

    private final GetTodayActivitiesUseCase getTodayActivitiesUseCase;

    @Operation(summary = "오늘의 기록 타임라인 (#100 M2)",
            description = "선택 어르신의 하루 활동(인지 훈련 완료·추억 답변 도착·하루 한마디 열람)을 시각순으로 조회한다. "
                    + "date 미지정 또는 'today'면 오늘(KST), 그 외 YYYY-MM-DD.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "인가 실패 — CARE_ACCESS_DENIED")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<TodayActivitiesResponse>> activities(
            @RequestAttribute UUID guardianId,
            @PathVariable UUID elderId,
            @RequestParam(required = false) String date) {

        LocalDate selectedDate = parseDate(date);
        var entries = getTodayActivitiesUseCase.execute(guardianId, elderId, selectedDate);
        return ResponseEntity.ok(ApiResponse.ok(TodayActivitiesResponse.from(selectedDate, entries)));
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isBlank() || date.equalsIgnoreCase("today")) {
            return getTodayActivitiesUseCase.today();
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "date는 YYYY-MM-DD 형식이어야 합니다.");
        }
    }
}
