package com.memeboo2.haemi.guardian.presentation;

import com.memeboo2.haemi.common.web.ApiResponse;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.profile.application.GetGuardianProfileUseCase;
import com.memeboo2.haemi.guardian.profile.application.GetGuardianProfileUseCase.ElderCard;
import com.memeboo2.haemi.guardian.profile.application.GetGuardianProfileUseCase.GuardianProfile;
import com.memeboo2.haemi.guardian.profile.application.UpdateGuardianProfileUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "프로필 (보호자)", description = "보호자 프로필 조회 및 수정")
@RestController
@RequestMapping("/api/v1/guardian/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final GetGuardianProfileUseCase getGuardianProfileUseCase;
    private final UpdateGuardianProfileUseCase updateGuardianProfileUseCase;

    public record ElderCardResponse(UUID elderId, String name, LocalDate birthDate, GuardianRole role, String roleLabel) {
        static ElderCardResponse from(ElderCard c) {
            return new ElderCardResponse(c.elderId(), c.name(), c.birthDate(), c.role(), c.role().getLabel());
        }
    }

    public record ProfileResponse(
            UUID userId,
            String name,
            String loginId,
            String phone,
            String birthDate,
            String profileImageUrl,
            List<ElderCardResponse> elders
    ) {
        static ProfileResponse from(GuardianProfile p) {
            return new ProfileResponse(
                    p.userId(), p.name(), p.loginId(), p.phone(), p.birthDate(), p.profileImageUrl(),
                    p.elders().stream().map(ElderCardResponse::from).toList()
            );
        }
    }

    public record UpdateProfileRequest(
            @Size(min = 1, max = 100) @jakarta.validation.constraints.Pattern(regexp = ".*\\S.*") String name,
            LocalDate birthDate,
            @Size(min = 3, max = 20) String loginId,
            UUID profileImageMediaRefId,
            Map<UUID, GuardianRole> elderRoles
    ) {}

    @Operation(summary = "보호자 프로필 조회", description = "가족 정보는 GET /api/v1/guardian/families/my에서 조회한다 (중복 제거).")
    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @RequestAttribute UUID guardianId) {
        GuardianProfile profile = getGuardianProfileUseCase.execute(guardianId);
        return ResponseEntity.ok(ApiResponse.ok(ProfileResponse.from(profile)));
    }

    @Operation(summary = "보호자 프로필 수정 (이름·생년월일·아이디·어르신별 역할)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "역할 값 누락 — INVALID_INPUT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 링크 아님 — NOT_RESOURCE_OWNER"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 사용 중인 아이디 — LOGIN_ID_ALREADY_TAKEN")
    })
    @PatchMapping
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @RequestAttribute UUID guardianId,
            @RequestBody @Valid UpdateProfileRequest req) {
        Map<UUID, GuardianRole> roles = req.elderRoles() != null ? req.elderRoles() : Map.of();
        updateGuardianProfileUseCase.execute(
                guardianId, req.name(), req.birthDate(), req.loginId(), req.profileImageMediaRefId(), roles);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
