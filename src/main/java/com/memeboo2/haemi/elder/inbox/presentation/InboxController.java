package com.memeboo2.haemi.elder.inbox.presentation;

import com.memeboo2.haemi.common.web.ApiResponse;
import com.memeboo2.haemi.elder.inbox.application.GetInboxUseCase;
import com.memeboo2.haemi.elder.inbox.application.MarkReadUseCase;
import com.memeboo2.haemi.elder.inbox.presentation.dto.InboxItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "하루 한마디 수신함 (어르신)", description = "어르신이 받은 하루 한마디 조회")
@RestController
@RequestMapping("/api/v1/elder/inbox")
@RequiredArgsConstructor
public class InboxController {

    private final GetInboxUseCase getInboxUseCase;
    private final MarkReadUseCase markReadUseCase;

    @Operation(summary = "오늘 받은 하루 한마디 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<List<InboxItem>>> list(
            @RequestAttribute UUID elderId) {

        List<InboxItem> result = getInboxUseCase.execute(elderId)
                .stream().map(InboxItem::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "하루 한마디 읽음 처리")
    @PostMapping("/{dailyCareId}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @RequestAttribute UUID elderId,
            @PathVariable UUID dailyCareId) {

        markReadUseCase.execute(elderId, dailyCareId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
