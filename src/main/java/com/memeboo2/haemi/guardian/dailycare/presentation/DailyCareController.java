package com.memeboo2.haemi.guardian.dailycare.presentation;

import com.memeboo2.haemi.common.web.ApiResponse;
import com.memeboo2.haemi.guardian.dailycare.application.GetSentDailyCareHistoryUseCase;
import com.memeboo2.haemi.guardian.dailycare.application.SendDailyCareUseCase;
import com.memeboo2.haemi.guardian.dailycare.presentation.dto.SendTextRequest;
import com.memeboo2.haemi.guardian.dailycare.presentation.dto.SendVoiceRequest;
import com.memeboo2.haemi.guardian.dailycare.presentation.dto.SentDailyCareItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "하루 한마디 (보호자)", description = "보호자가 어르신께 하루 한마디 전송")
@RestController
@RequestMapping("/api/v1/guardian/elders/{elderId}/daily-care")
@RequiredArgsConstructor
public class DailyCareController {

    private final SendDailyCareUseCase sendDailyCareUseCase;
    private final GetSentDailyCareHistoryUseCase getSentDailyCareHistoryUseCase;

    @Operation(summary = "텍스트 하루 한마디 전송")
    @PostMapping("/text")
    public ResponseEntity<ApiResponse<UUID>> sendText(
            @RequestAttribute UUID guardianId,
            @PathVariable UUID elderId,
            @Valid @RequestBody SendTextRequest req) {

        UUID id = sendDailyCareUseCase.sendText(guardianId, elderId, req.text());
        return ResponseEntity
                .created(URI.create("/api/v1/elder/inbox/" + id))
                .body(ApiResponse.ok(id));
    }

    @Operation(summary = "음성 하루 한마디 전송")
    @PostMapping("/voice")
    public ResponseEntity<ApiResponse<UUID>> sendVoice(
            @RequestAttribute UUID guardianId,
            @PathVariable UUID elderId,
            @Valid @RequestBody SendVoiceRequest req) {

        UUID id = sendDailyCareUseCase.sendVoice(guardianId, elderId, req.mediaRefId(), req.durationSeconds());
        return ResponseEntity
                .created(URI.create("/api/v1/elder/inbox/" + id))
                .body(ApiResponse.ok(id));
    }

    @Operation(summary = "내가 보낸 하루 한마디 이력", description = "R6: 발신자 본인 것만 반환한다.")
    @GetMapping("/sent")
    public ResponseEntity<ApiResponse<List<SentDailyCareItem>>> sentHistory(
            @RequestAttribute UUID guardianId,
            @PathVariable UUID elderId) {

        List<SentDailyCareItem> result = getSentDailyCareHistoryUseCase.execute(guardianId, elderId)
                .stream().map(SentDailyCareItem::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
