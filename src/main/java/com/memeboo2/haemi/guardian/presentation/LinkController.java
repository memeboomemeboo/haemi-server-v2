package com.memeboo2.haemi.guardian.presentation;

import com.memeboo2.haemi.guardian.eldermanagement.application.ChangeGuardianRoleUseCase;
import com.memeboo2.haemi.guardian.eldermanagement.application.UnlinkGuardianUseCase;
import com.memeboo2.haemi.guardian.presentation.dto.ChangeRoleRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "보호자-어르신 링크", description = "링크 해제 및 역할 변경")
@RestController
@RequestMapping("/api/v1/guardian/elders/{elderId}/link")
@RequiredArgsConstructor
public class LinkController {

    private final UnlinkGuardianUseCase unlinkUseCase;
    private final ChangeGuardianRoleUseCase changeRoleUseCase;

    @Operation(summary = "본인 링크 해제 (R8)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "해제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 링크 아님 — NOT_RESOURCE_OWNER"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "마지막 보호자 — LAST_GUARDIAN_CANNOT_LEAVE")
    })
    @DeleteMapping
    public ResponseEntity<Void> unlink(
            @RequestAttribute UUID guardianId,
            @PathVariable UUID elderId) {
        unlinkUseCase.execute(guardianId, elderId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "어르신에 대한 본인 역할 변경")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 링크 아님 — NOT_RESOURCE_OWNER")
    })
    @PatchMapping("/role")
    public ResponseEntity<Void> changeRole(
            @RequestAttribute UUID guardianId,
            @PathVariable UUID elderId,
            @Valid @RequestBody ChangeRoleRequest req) {
        changeRoleUseCase.execute(guardianId, elderId, req.role());
        return ResponseEntity.noContent().build();
    }
}
