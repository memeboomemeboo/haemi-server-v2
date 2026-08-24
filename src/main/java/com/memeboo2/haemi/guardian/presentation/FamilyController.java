package com.memeboo2.haemi.guardian.presentation;

import com.memeboo2.haemi.common.web.ApiResponse;
import com.memeboo2.haemi.guardian.family.application.CreateFamilyUseCase;
import com.memeboo2.haemi.guardian.family.application.JoinFamilyUseCase;
import com.memeboo2.haemi.guardian.presentation.dto.CreateFamilyRequest;
import com.memeboo2.haemi.guardian.presentation.dto.CreateFamilyResponse;
import com.memeboo2.haemi.guardian.presentation.dto.JoinFamilyRequest;
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

    @Operation(summary = "가족 생성", description = "생성 시 초대 코드가 함께 발급된다 (D4).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 가족에 속함 — FAMILY_CAPACITY_EXCEEDED")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<CreateFamilyResponse>> create(
            @RequestAttribute UUID guardianId,
            @Valid @RequestBody CreateFamilyRequest req) {
        CreateFamilyUseCase.Result result =
                createFamilyUseCase.execute(guardianId, req.name(), req.memo(), req.profileImageMediaRefId());
        return ResponseEntity.created(URI.create("/api/v1/guardian/families/" + result.familyId()))
                .body(ApiResponse.ok(CreateFamilyResponse.from(result)));
    }

    @Operation(summary = "초대 코드로 기존 가족에 합류", description = "어르신 계정은 /api/v1/guardian/** 자체를 호출할 수 없어 초대 대상에서 자동으로 제외된다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "합류 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "유효하지 않은 초대 코드 — RESOURCE_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "상한 초과 또는 이미 소속 — FAMILY_CAPACITY_EXCEEDED")
    })
    @PostMapping("/join")
    public ResponseEntity<Void> join(
            @RequestAttribute UUID guardianId,
            @Valid @RequestBody JoinFamilyRequest req) {
        joinFamilyUseCase.execute(guardianId, req.inviteCode());
        return ResponseEntity.noContent().build();
    }
}
