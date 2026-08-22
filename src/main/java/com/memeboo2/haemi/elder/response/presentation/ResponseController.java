package com.memeboo2.haemi.elder.response.presentation;

import com.memeboo2.haemi.common.web.ApiResponse;
import com.memeboo2.haemi.elder.response.application.CreateResponseUseCase;
import com.memeboo2.haemi.elder.response.application.GetResponsesUseCase;
import com.memeboo2.haemi.elder.response.presentation.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "추억 답변 (어르신)", description = "마음 전하기·댓글·음성 답변")
@RestController
@RequestMapping("/api/v1/elder/memories/{memoryId}/responses")
@RequiredArgsConstructor
public class ResponseController {

    private final CreateResponseUseCase createResponseUseCase;
    private final GetResponsesUseCase getResponsesUseCase;

    @Operation(summary = "마음 전하기")
    @PostMapping("/emotion")
    public ResponseEntity<ApiResponse<UUID>> emotion(
            @RequestAttribute UUID elderId,
            @PathVariable UUID memoryId,
            @Valid @RequestBody EmotionRequest req) {

        UUID id = createResponseUseCase.emotion(elderId, memoryId, req.emotions());
        return ResponseEntity.created(URI.create("/api/v1/elder/memories/" + memoryId + "/responses/" + id))
                .body(ApiResponse.ok(id));
    }

    @Operation(summary = "텍스트 댓글")
    @PostMapping("/text")
    public ResponseEntity<ApiResponse<UUID>> text(
            @RequestAttribute UUID elderId,
            @PathVariable UUID memoryId,
            @Valid @RequestBody TextRequest req) {

        UUID id = createResponseUseCase.text(elderId, memoryId, req.text());
        return ResponseEntity.created(URI.create("/api/v1/elder/memories/" + memoryId + "/responses/" + id))
                .body(ApiResponse.ok(id));
    }

    @Operation(summary = "이미지 댓글")
    @PostMapping("/image")
    public ResponseEntity<ApiResponse<UUID>> image(
            @RequestAttribute UUID elderId,
            @PathVariable UUID memoryId,
            @Valid @RequestBody MediaRefRequest req) {

        UUID id = createResponseUseCase.image(elderId, memoryId, req.mediaRefId());
        return ResponseEntity.created(URI.create("/api/v1/elder/memories/" + memoryId + "/responses/" + id))
                .body(ApiResponse.ok(id));
    }

    @Operation(summary = "음성 메시지")
    @PostMapping("/voice")
    public ResponseEntity<ApiResponse<UUID>> voice(
            @RequestAttribute UUID elderId,
            @PathVariable UUID memoryId,
            @Valid @RequestBody MediaRefRequest req) {

        UUID id = createResponseUseCase.voice(elderId, memoryId, req.mediaRefId());
        return ResponseEntity.created(URI.create("/api/v1/elder/memories/" + memoryId + "/responses/" + id))
                .body(ApiResponse.ok(id));
    }

    @Operation(summary = "내 답변 목록 조회 (어르신 본인)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ResponseSummary>>> list(
            @RequestAttribute UUID elderId,
            @PathVariable UUID memoryId) {

        List<ResponseSummary> result = getResponsesUseCase.execute(elderId, memoryId)
                .stream().map(ResponseSummary::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
