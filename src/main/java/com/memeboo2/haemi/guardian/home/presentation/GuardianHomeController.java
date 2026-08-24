package com.memeboo2.haemi.guardian.home.presentation;

import com.memeboo2.haemi.common.web.ApiResponse;
import com.memeboo2.haemi.guardian.home.application.GetGuardianHomeUseCase;
import com.memeboo2.haemi.guardian.home.presentation.dto.GuardianHomeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "홈 (보호자)", description = "보호자 홈 화면 조합")
@RestController
@RequestMapping("/api/v1/guardian/home")
@RequiredArgsConstructor
public class GuardianHomeController {

    private final GetGuardianHomeUseCase getGuardianHomeUseCase;

    @Operation(summary = "보호자 홈 화면 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<GuardianHomeResponse>> home(
            @RequestAttribute UUID guardianId) {

        GuardianHomeResponse result = GuardianHomeResponse.from(getGuardianHomeUseCase.execute(guardianId));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
