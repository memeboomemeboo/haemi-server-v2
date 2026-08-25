package com.memeboo2.haemi.elder.training.presentation;

import com.memeboo2.haemi.common.web.ApiResponse;
import com.memeboo2.haemi.elder.training.application.CompleteTrainingSessionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "인지 훈련 (어르신)", description = "오늘의 훈련 완료 처리")
@RestController
@RequestMapping("/api/v1/elder/training-sessions")
@RequiredArgsConstructor
public class TrainingSessionController {

    private final CompleteTrainingSessionUseCase completeTrainingSessionUseCase;

    @Operation(summary = "오늘의 훈련 완료", description = "출석과 보호자 리포트 스냅샷을 적재한다. 같은 날 반복 호출해도 한 번만 기록된다.")
    @PostMapping("/today/complete")
    public ResponseEntity<ApiResponse<LocalDate>> completeToday(
            @RequestAttribute("elderUserId") UUID elderUserId) {

        return ResponseEntity.ok(ApiResponse.ok(completeTrainingSessionUseCase.completeToday(elderUserId)));
    }
}
