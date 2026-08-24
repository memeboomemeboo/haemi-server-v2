package com.memeboo2.haemi.guardian.presentation;

import com.memeboo2.haemi.common.web.ApiResponse;
import com.memeboo2.haemi.guardian.family.application.CreateFamilyUseCase;
import com.memeboo2.haemi.guardian.family.application.GetFamilyDetailUseCase;
import com.memeboo2.haemi.guardian.family.application.JoinFamilyUseCase;
import com.memeboo2.haemi.guardian.presentation.dto.CreateFamilyRequest;
import com.memeboo2.haemi.guardian.presentation.dto.CreateFamilyResponse;
import com.memeboo2.haemi.guardian.presentation.dto.FamilyDetailResponse;
import com.memeboo2.haemi.guardian.presentation.dto.JoinFamilyRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@Tag(name = "가족", description = "가족 생성 및 멤버 관리")
@RestController
@RequestMapping("/api/v1/guardian/families")
@RequiredArgsConstructor
public class FamilyController {

    private final CreateFamilyUseCase createFamilyUseCase;
    private final JoinFamilyUseCase joinFamilyUseCase;
    private final GetFamilyDetailUseCase getFamilyDetailUseCase;

    @Operation(summary = "가족 생성", description = "생성 시 초대 코드가 함께 발급된다 (D4).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 가족에 속함 — FAMILY_CAPACITY_EXCEEDED")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<CreateFamilyResponse>> create(
            @RequestAttribute UUID guardianId,
            @Valid @RequestBody CreateFamilyRequest req) {
        CreateFamilyUseCase.Result result =
                createFamilyUseCase.execute(guardianId, req.name(), req.memo(), req.profileImageMediaRefId());
        return ResponseEntity.created(URI.create("/api/v1/guardian/families/" + result.familyId()))
                .body(ApiResponse.ok(CreateFamilyResponse.from(result)));
    }

    @Operation(summary = "초대 코드로 기존 가족에 합류", description = "어르신 계정은 /api/v1/guardian/** 자체를 호출할 수 없어 초대 대상에서 자동으로 제외된다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "합류 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "유효하지 않은 초대 코드 — RESOURCE_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "상한 초과 또는 이미 소속 — FAMILY_CAPACITY_EXCEEDED")
    })
    @PostMapping("/join")
    public ResponseEntity<Void> join(
            @RequestAttribute UUID guardianId,
            @Valid @RequestBody JoinFamilyRequest req) {
        joinFamilyUseCase.execute(guardianId, req.inviteCode());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "내 가족 조회 (D10)",
            description = "가족 요약·구성원·어르신 목록을 한 응답으로 내려준다. 화면 분할은 프론트가 담당한다. "
                    + "소속 가족이 없으면 404가 아니라 data: null로 응답한다. "
                    + "elderId를 지정하면 그 어르신 기준으로 다른 보호자의 관계 라벨(guardians[].role)을 계산하고, "
                    + "생략하면 어르신이 정확히 1명일 때만 그 어르신을 기준으로 삼는다 (그 외에는 null).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공 (미소속 시 data: null)")
    })
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<FamilyDetailResponse>> myFamily(
            @RequestAttribute UUID guardianId,
            @RequestParam(required = false) UUID elderId) {
        FamilyDetailResponse response = getFamilyDetailUseCase.execute(guardianId, elderId)
                .map(FamilyDetailResponse::from)
                .orElse(null);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
