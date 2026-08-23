package com.memeboo2.haemi.guardian.presentation;

import com.memeboo2.haemi.common.web.ApiResponse;
import com.memeboo2.haemi.guardian.eldermanagement.application.RegisterElderAccountUseCase;
import com.memeboo2.haemi.guardian.presentation.dto.RegisterElderRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@Tag(name = "어르신", description = "어르신 계정 등록 및 관리")
@RestController
@RequestMapping("/api/v1/guardian/elders")
@RequiredArgsConstructor
public class ElderController {

    private final RegisterElderAccountUseCase registerElderAccountUseCase;

    @Operation(summary = "어르신 등록 (ACC-REG-002)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "어르신 상한 초과 — FAMILY_CAPACITY_EXCEEDED")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<UUID>> register(
            @RequestAttribute UUID guardianId,
            @Valid @RequestBody RegisterElderRequest req) {
        UUID elderId = registerElderAccountUseCase.execute(
                guardianId, req.familyId(), req.name(), req.birthDate(), req.loginId(),
                req.password(), req.pin(), req.phone(), req.gender());
        return ResponseEntity.created(URI.create("/api/v1/guardian/elders/" + elderId))
                .body(ApiResponse.ok(elderId));
    }
}
