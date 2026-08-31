package com.memeboo2.haemi.auth.presentation;

import com.memeboo2.haemi.auth.account.application.CheckLoginIdAvailabilityUseCase;
import com.memeboo2.haemi.auth.account.application.RegisterGuardianUseCase;
import com.memeboo2.haemi.auth.verification.application.EmailVerificationUseCase;
import com.memeboo2.haemi.auth.session.application.LoginUseCase;
import com.memeboo2.haemi.auth.session.application.LogoutUseCase;
import com.memeboo2.haemi.auth.session.application.RefreshTokenUseCase;
import com.memeboo2.haemi.common.security.JwtPrincipal;
import com.memeboo2.haemi.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "인증", description = "회원가입, 로그인, 로그아웃")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final RegisterGuardianUseCase registerGuardianUseCase;
    private final CheckLoginIdAvailabilityUseCase checkLoginIdAvailabilityUseCase;
    private final EmailVerificationUseCase emailVerificationUseCase;
    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;

    public record GuardianRegisterRequest(
            @NotBlank @Size(max = 50) String name,
            @NotBlank @Size(min = 4, max = 50) String loginId,
            @NotBlank @Size(min = 8, max = 50) String password,
            @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}") String birthDate,
            @NotBlank @Pattern(regexp = "\\d{6}") String pin,
            @Size(max = 20) String phone,
            @Email @Size(max = 255) String email,
            UUID emailVerificationId
    ) {}

    public record LoginIdAvailabilityResponse(
            @Schema(description = "확인한 loginId") String loginId,
            @Schema(description = "해당 loginId가 아직 사용 가능하면 true") boolean available) {}

    public record EmailVerificationRequest(@NotBlank @Email @Size(max = 255) String email) {}

    public record EmailVerificationConfirmRequest(@NotBlank @Pattern(regexp = "\\d{6}") String code) {}

    public record LoginRequest(
            @NotBlank String loginId,
            String password,
            @Pattern(regexp = "\\d{6}|") String pin,
            @NotBlank @Size(max = 100) String deviceId
    ) {}

    public record LogoutRequest(@NotBlank @Size(max = 100) String deviceId) {}

    public record RefreshRequest(
            @NotBlank String refreshToken,
            @NotBlank @Size(max = 100) String deviceId
    ) {}

    public record TokenResponse(String accessToken, String refreshToken) {}

    public record RegisterResponse(UUID userId) {}

    @Operation(summary = "보호자 회원가입")
    @PostMapping("/guardians/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> registerGuardian(
            @RequestBody @Valid GuardianRegisterRequest req) {
        UUID userId = registerGuardianUseCase.execute(
                req.name(), req.loginId(), req.password(), req.birthDate(), req.pin(),
                req.phone(), req.email(), req.emailVerificationId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(new RegisterResponse(userId)));
    }

    @Operation(summary = "아이디 중복 확인",
            description = "제출 전 loginId 사용 가능 여부를 조회한다. 회원가입·프로필 수정·어르신 등록의 '중복 확인' 버튼용.")
    @GetMapping("/login-id/availability")
    public ResponseEntity<ApiResponse<LoginIdAvailabilityResponse>> checkLoginIdAvailability(
            @RequestParam @NotBlank @Size(min = 4, max = 50) String loginId) {
        boolean available = checkLoginIdAvailabilityUseCase.isAvailable(loginId);
        return ResponseEntity.ok(ApiResponse.ok(new LoginIdAvailabilityResponse(loginId, available)));
    }

    @Operation(summary = "이메일 인증번호 발송")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "발송 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "재발송 횟수 초과 — AUTH_VERIFICATION_RESEND_LIMITED")
    })
    @PostMapping("/email-verifications")
    public ResponseEntity<ApiResponse<UUID>> requestEmailVerification(
            @RequestBody @Valid EmailVerificationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(emailVerificationUseCase.request(req.email())));
    }

    @Operation(summary = "이메일 인증번호 확인")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "확인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "인증번호 불일치 — INVALID_INPUT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "확인 시도 횟수 초과 — AUTH_VERIFICATION_LOCKED")
    })
    @PostMapping("/email-verifications/{verificationId}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmEmailVerification(
            @PathVariable UUID verificationId,
            @RequestBody @Valid EmailVerificationConfirmRequest req) {
        emailVerificationUseCase.confirm(verificationId, req.code());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "로그인")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "아이디·비밀번호 불일치 — INVALID_CREDENTIALS"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "423", description = "로그인 시도 횟수 초과로 계정 잠김 — AUTH_ACCOUNT_LOCKED")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @RequestBody @Valid LoginRequest req) {
        if ((req.password() == null || req.password().isBlank()) && (req.pin() == null || req.pin().isBlank())) {
            throw new com.memeboo2.haemi.common.error.DomainException(
                    com.memeboo2.haemi.common.error.ErrorCode.INVALID_INPUT,
                    "비밀번호 또는 PIN을 입력해주세요.");
        }
        LoginUseCase.TokenPair pair = loginUseCase.execute(req.loginId(), req.password(), req.pin(), req.deviceId());
        return ResponseEntity.ok(ApiResponse.ok(new TokenResponse(pair.accessToken(), pair.refreshToken())));
    }

    @Operation(summary = "액세스 토큰 재발급", description = "refresh 토큰과 deviceId로 새 access·refresh 토큰을 발급한다. refresh 토큰은 회전된다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 refresh 토큰 — AUTH_REFRESH_TOKEN_INVALID")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @RequestBody @Valid RefreshRequest req) {
        LoginUseCase.TokenPair pair = refreshTokenUseCase.execute(req.refreshToken(), req.deviceId());
        return ResponseEntity.ok(ApiResponse.ok(new TokenResponse(pair.accessToken(), pair.refreshToken())));
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestBody @Valid LogoutRequest req) {
        logoutUseCase.execute(principal.userId(), req.deviceId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
