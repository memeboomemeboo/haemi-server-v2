package com.memeboo2.haemi.elder.home.presentation;

import com.memeboo2.haemi.common.web.ApiResponse;
import com.memeboo2.haemi.elder.home.application.GetElderHomeUseCase;
import com.memeboo2.haemi.elder.home.presentation.dto.ElderHomeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "홈 (어르신)", description = "어르신 홈 화면 조합")
@RestController
@RequestMapping("/api/v1/elder/home")
@RequiredArgsConstructor
public class ElderHomeController {

    private final GetElderHomeUseCase getElderHomeUseCase;

    @Operation(summary = "어르신 홈 화면 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<ElderHomeResponse>> home(
            @RequestAttribute UUID elderId) {

        ElderHomeResponse result = ElderHomeResponse.from(getElderHomeUseCase.execute(elderId));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
