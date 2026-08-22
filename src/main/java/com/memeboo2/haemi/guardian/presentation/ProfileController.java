package com.memeboo2.haemi.guardian.presentation;

import com.memeboo2.haemi.common.web.ApiResponse;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.profile.application.GetGuardianProfileUseCase;
import com.memeboo2.haemi.guardian.profile.application.GetGuardianProfileUseCase.ElderCard;
import com.memeboo2.haemi.guardian.profile.application.GetGuardianProfileUseCase.FamilyInfo;
import com.memeboo2.haemi.guardian.profile.application.GetGuardianProfileUseCase.GuardianProfile;
import com.memeboo2.haemi.guardian.profile.application.UpdateGuardianProfileUseCase;
import io.swagger.v3.oas.annotations.Operation;
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
import java.util.stream.Collectors;

@Tag(name = "프로필 (보호자)", description = "보호자 프로필 조회 및 수정")
@RestController
@RequestMapping("/api/v1/guardian/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final GetGuardianProfileUseCase getGuardianProfileUseCase;
    private final UpdateGuardianProfileUseCase updateGuardianProfileUseCase;

    public record ElderCardResponse(UUID elderId, String name, LocalDate birthDate, GuardianRole role) {
        static ElderCardResponse from(ElderCard c) {
            return new ElderCardResponse(c.elderId(), c.name(), c.birthDate(), c.role());
        }
    }

    public record FamilyResponse(UUID familyId, String name) {
        static FamilyResponse from(FamilyInfo f) {
            return f == null ? null : new FamilyResponse(f.familyId(), f.name());
        }
    }

    public record ProfileResponse(
            UUID userId,
            String name,
            String loginId,
            String phone,
            FamilyResponse family,
            List<ElderCardResponse> elders
    ) {
        static ProfileResponse from(GuardianProfile p) {
            return new ProfileResponse(
                    p.userId(), p.name(), p.loginId(), p.phone(),
                    FamilyResponse.from(p.family()),
                    p.elders().stream().map(ElderCardResponse::from).toList()
            );
        }
    }

    public record UpdateProfileRequest(
            @Size(min = 3, max = 20) String loginId,
            Map<UUID, GuardianRole> elderRoles
    ) {}

    @Operation(summary = "보호자 프로필 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @RequestAttribute UUID guardianId) {
        GuardianProfile profile = getGuardianProfileUseCase.execute(guardianId);
        return ResponseEntity.ok(ApiResponse.ok(ProfileResponse.from(profile)));
    }

    @Operation(summary = "보호자 프로필 수정 (아이디·어르신별 역할)")
    @PatchMapping
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @RequestAttribute UUID guardianId,
            @RequestBody @Valid UpdateProfileRequest req) {
        Map<UUID, GuardianRole> roles = req.elderRoles() != null ? req.elderRoles() : Map.of();
        updateGuardianProfileUseCase.execute(guardianId, req.loginId(), roles);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
