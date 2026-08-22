package com.memeboo2.haemi.auth.presentation;

import com.memeboo2.haemi.auth.account.application.RegisterGuardianUseCase;
import com.memeboo2.haemi.auth.session.application.LoginUseCase;
import com.memeboo2.haemi.auth.session.application.LogoutUseCase;
import com.memeboo2.haemi.common.security.JwtPrincipal;
import com.memeboo2.haemi.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "인증", description = "회원가입, 로그인, 로그아웃")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterGuardianUseCase registerGuardianUseCase;
    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;

    public record GuardianRegisterRequest(
            @NotBlank @Size(max = 50) String name,
            @NotBlank @Size(min = 4, max = 50) String loginId,
            @NotBlank @Size(min = 8, max = 50) String password
    ) {}

    public record LoginRequest(
            @NotBlank String loginId,
            @NotBlank String password
    ) {}

    public record TokenResponse(String accessToken, String refreshToken) {}

    public record RegisterResponse(UUID userId) {}

    @Operation(summary = "보호자 회원가입")
    @PostMapping("/guardians/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> registerGuardian(
            @RequestBody @Valid GuardianRegisterRequest req) {
        UUID userId = registerGuardianUseCase.execute(req.name(), req.loginId(), req.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(new RegisterResponse(userId)));
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @RequestBody @Valid LoginRequest req) {
        LoginUseCase.TokenPair pair = loginUseCase.execute(req.loginId(), req.password());
        return ResponseEntity.ok(ApiResponse.ok(new TokenResponse(pair.accessToken(), pair.refreshToken())));
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal JwtPrincipal principal) {
        logoutUseCase.execute(principal.userId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
