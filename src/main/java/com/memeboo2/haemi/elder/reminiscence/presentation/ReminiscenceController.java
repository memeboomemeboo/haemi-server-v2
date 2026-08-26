package com.memeboo2.haemi.elder.reminiscence.presentation;

import com.memeboo2.haemi.common.security.JwtPrincipal;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.common.web.ApiResponse;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.elder.reminiscence.application.ReminiscenceService;
import com.memeboo2.haemi.elder.reminiscence.domain.GeneratedReminiscence;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "AI 회상 콘텐츠 (어르신)", description = "매일 08:00 생성되는 개인화 회상 콘텐츠")
@RestController
@RequestMapping("/api/v1/elder/reminiscence")
@RequiredArgsConstructor
public class ReminiscenceController {

    private final CareAccessQuery careAccessQuery;
    private final ReminiscenceService reminiscenceService;
    private final HaemiClock clock;

    @Operation(summary = "오늘의 개인화 회상 콘텐츠 조회 (어르신 본인)",
            description = "배치 미생성 시 즉석 생성해 반환한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회/생성 성공")
    })
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<ReminiscenceResponse>> today(
            @AuthenticationPrincipal JwtPrincipal principal) {
        UUID elderId = careAccessQuery.elderIdForUser(principal.userId());
        LocalDate today = clock.today();

        GeneratedReminiscence content = reminiscenceService.findForElder(elderId, today)
                .orElseGet(() -> reminiscenceService.generateForElder(elderId, today));

        return ResponseEntity.ok(ApiResponse.ok(new ReminiscenceResponse(
                content.getContentDate(), content.getContent(), content.isAiGenerated())));
    }

    public record ReminiscenceResponse(LocalDate date, String content, boolean aiGenerated) {}
}
