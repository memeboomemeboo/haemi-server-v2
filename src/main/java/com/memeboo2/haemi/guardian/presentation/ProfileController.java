package com.memeboo2.haemi.guardian.presentation;

import com.memeboo2.haemi.common.web.ApiResponse;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.profile.application.GetGuardianProfileUseCase;
import com.memeboo2.haemi.guardian.profile.application.GetGuardianProfileUseCase.ElderCard;
import com.memeboo2.haemi.guardian.profile.application.GetGuardianProfileUseCase.GuardianProfile;
import com.memeboo2.haemi.guardian.profile.application.UpdateGuardianProfileUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(name = "ProfileElderCardResponse")
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
            @Schema(description = "보호자 이름. 전달하면 변경한다. 공백만으로는 구성할 수 없다.", example = "박승아")
            @Size(min = 1, max = 100) @jakarta.validation.constraints.Pattern(regexp = ".*\\S.*") String name,
            @Schema(description = "생년월일. 전달하면 변경한다. 1920-01-01부터 요청 당일(KST)까지 허용한다.", example = "1985-06-10")
            LocalDate birthDate,
            @Schema(description = "전화번호. 전달하면 변경한다. 최대 20자다.", example = "010-9999-8888")
            @Size(max = 20) String phone,
            @Schema(description = "로그인 아이디. 전달하면 변경한다. 3~20자이며 다른 계정과 중복될 수 없다.", example = "jeongeun")
            @Size(min = 3, max = 20) String loginId,
            @Schema(description = "업로드·확정한 프로필 이미지의 mediaRefId. 전달하면 해당 이미지로 변경한다.", example = "a2c6d96d-b999-4c5e-aa44-3ca02aaee8b3")
            UUID profileImageMediaRefId,
            @Schema(description = "어르신 ID를 키로 하는 보호자 역할 맵. 전달한 항목의 역할만 변경한다.", example = "{\"a2c6d96d-b999-4c5e-aa44-3ca02aaee8b3\":\"DAUGHTER\"}")
            Map<UUID, GuardianRole> elderRoles
    ) {}

    @Operation(summary = "보호자 프로필 조회", description = "가족 정보는 GET /api/v1/guardian/families/my에서 조회한다 (중복 제거).")
    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @RequestAttribute UUID guardianId) {
        GuardianProfile profile = getGuardianProfileUseCase.execute(guardianId);
        return ResponseEntity.ok(ApiResponse.ok(ProfileResponse.from(profile)));
    }

    @Operation(summary = "보호자 프로필 수정 (이름·생년월일·전화번호·아이디·어르신별 역할)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "역할 값 누락 — INVALID_INPUT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 링크 아님 — NOT_RESOURCE_OWNER"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 사용 중인 아이디 — LOGIN_ID_ALREADY_TAKEN")
    })
    @PatchMapping
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @RequestAttribute UUID guardianId,
            @RequestBody @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "모든 필드는 선택값이며, 전달한 필드만 수정한다.")
            UpdateProfileRequest req) {
        Map<UUID, GuardianRole> roles = req.elderRoles() != null ? req.elderRoles() : Map.of();
        updateGuardianProfileUseCase.execute(
                guardianId, req.name(), req.birthDate(), req.phone(), req.loginId(), req.profileImageMediaRefId(), roles);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
