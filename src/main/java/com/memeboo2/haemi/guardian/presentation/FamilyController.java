package com.memeboo2.haemi.guardian.presentation;

import com.memeboo2.haemi.common.web.ApiResponse;
import com.memeboo2.haemi.guardian.family.application.CreateFamilyUseCase;
import com.memeboo2.haemi.guardian.family.application.JoinFamilyUseCase;
import com.memeboo2.haemi.guardian.presentation.dto.CreateFamilyRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@Tag(name = "가족", description = "가족 생성 및 멤버 관리")
@RestController
@RequestMapping("/api/v1/guardian/families")
@RequiredArgsConstructor
public class FamilyController {

    private final CreateFamilyUseCase createFamilyUseCase;
    private final JoinFamilyUseCase joinFamilyUseCase;

    @Operation(summary = "가족 생성")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 가족에 속함 — FAMILY_CAPACITY_EXCEEDED")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<UUID>> create(
            @RequestAttribute UUID guardianId,
            @Valid @RequestBody CreateFamilyRequest req) {
        UUID familyId = createFamilyUseCase.execute(guardianId, req.name());
        return ResponseEntity.created(URI.create("/api/v1/guardian/families/" + familyId))
                .body(ApiResponse.ok(familyId));
    }

    @Operation(summary = "기존 가족에 보호자 합류")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "합류 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "가족 없음 — RESOURCE_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "상한 초과 또는 이미 소속 — FAMILY_CAPACITY_EXCEEDED")
    })
    @PostMapping("/{familyId}/members")
    public ResponseEntity<Void> join(
            @RequestAttribute UUID guardianId,
            @PathVariable UUID familyId) {
        joinFamilyUseCase.execute(guardianId, familyId);
        return ResponseEntity.noContent().build();
    }
}
