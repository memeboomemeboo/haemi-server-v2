package com.memeboo2.haemi.guardian.presentation;

import com.memeboo2.haemi.auth.api.AccountCommand;
import com.memeboo2.haemi.common.web.ApiResponse;
import com.memeboo2.haemi.guardian.eldermanagement.application.RegisterElderUseCase;
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

    private final RegisterElderUseCase registerElderUseCase;
    private final AccountCommand accountCommand;

    @Operation(summary = "어르신 등록 (ACC-REG-002)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "어르신 상한 초과 — FAMILY_CAPACITY_EXCEEDED")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<UUID>> register(
            @RequestAttribute UUID guardianId,
            @Valid @RequestBody RegisterElderRequest req) {
        String birthDate = req.birthDate() != null ? req.birthDate().toString() : null;
        UUID elderUserId = accountCommand.createElderAccount(
                req.name(), req.loginId(), req.pin(), birthDate, req.phone());
        UUID elderId = registerElderUseCase.execute(
                guardianId, elderUserId, req.familyId(), req.name(), req.birthDate());
        return ResponseEntity.created(URI.create("/api/v1/guardian/elders/" + elderId))
                .body(ApiResponse.ok(elderId));
    }
}
