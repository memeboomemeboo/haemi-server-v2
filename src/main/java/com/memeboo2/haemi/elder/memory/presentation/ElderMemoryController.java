package com.memeboo2.haemi.elder.memory.presentation;

import com.memeboo2.haemi.common.web.ApiResponse;
import com.memeboo2.haemi.elder.memory.application.GetMemoriesUseCase;
import com.memeboo2.haemi.elder.memory.application.GetMemoryDetailUseCase;
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
}
