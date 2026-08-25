package com.memeboo2.haemi.elder.memory.presentation;

import com.memeboo2.haemi.common.web.ApiResponse;
import com.memeboo2.haemi.elder.memory.application.GetMemoriesUseCase;
import com.memeboo2.haemi.elder.memory.application.GetMemoryDetailUseCase;
import com.memeboo2.haemi.elder.memory.application.MarkMemoryViewedUseCase;
import com.memeboo2.haemi.elder.memory.presentation.dto.MemoryDetail;
import com.memeboo2.haemi.elder.memory.presentation.dto.MemorySummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "추억 목록 (어르신)", description = "어르신 본인에게 등록된 추억 조회")
@RestController
@RequestMapping("/api/v1/elder/memories")
@RequiredArgsConstructor
public class ElderMemoryController {

    private final GetMemoriesUseCase getMemoriesUseCase;
    private final GetMemoryDetailUseCase getMemoryDetailUseCase;
    private final MarkMemoryViewedUseCase markMemoryViewedUseCase;

    @Operation(summary = "추억 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MemorySummary>>> list(
            @RequestAttribute("elderUserId") UUID elderUserId) {

        List<MemorySummary> result = getMemoriesUseCase.execute(elderUserId)
                .stream().map(MemorySummary::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "추억 상세 조회")
    @GetMapping("/{memoryId}")
    public ResponseEntity<ApiResponse<MemoryDetail>> detail(
            @RequestAttribute("elderUserId") UUID elderUserId,
            @PathVariable UUID memoryId) {

        MemoryDetail result = MemoryDetail.from(getMemoryDetailUseCase.execute(elderUserId, memoryId));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "추억 열람 처리", description = "어르신이 추억을 열어봤음을 기록한다. 최초 열람 1회만 MemoryViewed 이벤트를 발행한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "열람 기록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "본인 추억이 아님 — RESOURCE_NOT_FOUND")
    })
    @PostMapping("/{memoryId}/viewed")
    public ResponseEntity<ApiResponse<Void>> markViewed(
            @RequestAttribute("elderUserId") UUID elderUserId,
            @PathVariable UUID memoryId) {

        markMemoryViewedUseCase.execute(elderUserId, memoryId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
