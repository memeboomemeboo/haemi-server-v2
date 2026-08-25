package com.memeboo2.haemi.elder.training.presentation;

import com.memeboo2.haemi.common.web.ApiResponse;
import com.memeboo2.haemi.elder.training.application.TrainingSessionUseCase;
import com.memeboo2.haemi.elder.training.presentation.dto.CompleteTrainingQuestionRequest;
import com.memeboo2.haemi.elder.training.presentation.dto.TrainingSessionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "인지 훈련 (어르신)", description = "CIST 세션 진입·이어하기·응답·결과 조회")
@RestController
@RequestMapping("/api/v1/elder/training/session")
@RequiredArgsConstructor
public class TrainingSessionController {

    private final TrainingSessionUseCase trainingSessionUseCase;

    @Operation(summary = "인지 훈련 세션 진입 또는 이어하기")
    @PostMapping("/enter")
    public ResponseEntity<ApiResponse<TrainingSessionResponse>> enter(
            @RequestAttribute("elderUserId") UUID elderUserId) {

        TrainingSessionResponse response = TrainingSessionResponse.from(trainingSessionUseCase.enter(elderUserId));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "현재 인지 훈련 문항 응답 제출")
    @PostMapping("/current-question/complete")
    public ResponseEntity<ApiResponse<TrainingSessionResponse>> completeCurrentQuestion(
            @RequestAttribute("elderUserId") UUID elderUserId,
            @RequestBody @Valid CompleteTrainingQuestionRequest request) {

        TrainingSessionResponse response = TrainingSessionResponse.from(
                trainingSessionUseCase.submitCurrentAnswer(
                        elderUserId,
                        request.sessionId(),
                        request.questionId(),
                        request.questionNumber(),
                        request.selectedOption(),
                        request.textAnswer(),
                        request.voiceMediaRefId()));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "오늘 완료한 인지 훈련 결과 조회")
    @GetMapping("/result")
    public ResponseEntity<ApiResponse<TrainingSessionResponse>> result(
            @RequestAttribute("elderUserId") UUID elderUserId) {

        TrainingSessionResponse response = TrainingSessionResponse.fromResult(
                trainingSessionUseCase.result(elderUserId));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
