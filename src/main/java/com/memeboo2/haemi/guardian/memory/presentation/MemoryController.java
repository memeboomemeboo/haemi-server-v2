package com.memeboo2.haemi.guardian.memory.presentation;

import com.memeboo2.haemi.common.web.ApiResponse;
import com.memeboo2.haemi.guardian.api.ResponseQuery;
import com.memeboo2.haemi.guardian.memory.application.*;
import com.memeboo2.haemi.guardian.memory.presentation.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "추억 앨범 (보호자)", description = "추억 등록·조회·수정·삭제")
@RestController
@RequestMapping("/api/v1/guardian/memories")
@RequiredArgsConstructor
public class MemoryController {

    private final RegisterMemoryUseCase registerMemoryUseCase;
    private final GetMemoriesUseCase getMemoriesUseCase;
    private final GetMemoryDetailUseCase getMemoryDetailUseCase;
    private final GetMemoryResponsesUseCase getMemoryResponsesUseCase;
    private final UpdateMemoryUseCase updateMemoryUseCase;
    private final DeleteMemoryUseCase deleteMemoryUseCase;

    @Operation(summary = "추억 등록")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력 오류 — INVALID_INPUT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "인가 실패 — CARE_ACCESS_DENIED")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<UUID>> register(
            @RequestAttribute UUID guardianId,
            @Valid @RequestBody RegisterMemoryRequest req) {

        UUID memoryId = registerMemoryUseCase.execute(
                guardianId, req.elderId(), req.title(), req.memo(),
                req.message(), req.memoryYear(), req.mediaRefIds());

        return ResponseEntity.created(URI.create("/api/v1/guardian/memories/" + memoryId))
                .body(ApiResponse.ok(memoryId));
    }

    @Operation(summary = "추억 목록 조회 (최근 1년)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "인가 실패 — CARE_ACCESS_DENIED")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<MemorySummaryResponse>>> list(
            @RequestAttribute UUID guardianId,
            @RequestParam UUID elderId) {

        List<MemorySummaryResponse> result = getMemoriesUseCase.execute(guardianId, elderId)
                .stream().map(MemorySummaryResponse::from).toList();

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "추억 상세 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "인가 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 추억")
    })
    @GetMapping("/{memoryId}")
    public ResponseEntity<ApiResponse<MemoryDetailResponse>> detail(
            @RequestAttribute UUID guardianId,
            @PathVariable UUID memoryId) {

        return ResponseEntity.ok(ApiResponse.ok(
                MemoryDetailResponse.from(getMemoryDetailUseCase.execute(guardianId, memoryId))));
    }

    @Operation(summary = "어르신 답변 조회")
    @GetMapping("/{memoryId}/responses")
    public ResponseEntity<ApiResponse<List<ResponseQuery.ResponseItem>>> responses(
            @RequestAttribute UUID guardianId,
            @PathVariable UUID memoryId) {
        return ResponseEntity.ok(ApiResponse.ok(getMemoryResponsesUseCase.execute(guardianId, memoryId)));
    }

    @Operation(summary = "추억 수정 (생성자 본인만)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "생성자 아님 — NOT_RESOURCE_OWNER"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 추억")
    })
    @PutMapping("/{memoryId}")
    public ResponseEntity<Void> update(
            @RequestAttribute UUID guardianId,
            @PathVariable UUID memoryId,
            @Valid @RequestBody UpdateMemoryRequest req) {

        updateMemoryUseCase.execute(guardianId, memoryId,
                req.title(), req.memo(), req.message(), req.memoryYear(), req.mediaRefIds());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "추억 삭제 (생성자 본인만)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "생성자 아님 — NOT_RESOURCE_OWNER"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 추억")
    })
    @DeleteMapping("/{memoryId}")
    public ResponseEntity<Void> delete(
            @RequestAttribute UUID guardianId,
            @PathVariable UUID memoryId) {

        deleteMemoryUseCase.execute(guardianId, memoryId);
        return ResponseEntity.noContent().build();
    }
}
