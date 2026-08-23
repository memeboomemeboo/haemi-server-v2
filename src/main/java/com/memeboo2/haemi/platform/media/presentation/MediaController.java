package com.memeboo2.haemi.platform.media.presentation;

import com.memeboo2.haemi.common.web.ApiResponse;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.security.JwtPrincipal;
import com.memeboo2.haemi.platform.media.application.ConfirmUploadUseCase;
import com.memeboo2.haemi.platform.media.application.RequestUploadUseCase;
import com.memeboo2.haemi.platform.media.presentation.dto.RequestUploadRequest;
import com.memeboo2.haemi.platform.media.presentation.dto.RequestUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@Tag(name = "미디어", description = "presigned URL 발급 및 업로드 확정")
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final RequestUploadUseCase requestUploadUseCase;
    private final ConfirmUploadUseCase confirmUploadUseCase;

    @Operation(summary = "업로드 URL 발급 (presigned PUT URL)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "허용되지 않는 포맷/크기 — INVALID_INPUT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "미인증")
    })
    @PostMapping("/upload-request")
    public ResponseEntity<ApiResponse<RequestUploadResponse>> requestUpload(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody RequestUploadRequest req) {

        validatePrincipalCanUpload(principal, req.mediaType());

        RequestUploadUseCase.Result result = requestUploadUseCase.request(
                principal.userId(), req.mediaType(), req.originalFilename(), req.contentType(),
                req.declaredSizeBytes(), req.declaredDurationSeconds());

        RequestUploadResponse body = new RequestUploadResponse(
                result.mediaRefId(), result.presignedUrl(), result.expiresAt());

        return ResponseEntity.created(URI.create("/api/v1/media/" + result.mediaRefId()))
                .body(ApiResponse.ok(body));
    }

    @Operation(summary = "업로드 확정 (클라이언트 업로드 완료 후 호출)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "확정 성공 — 서빙 URL 반환"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 미디어 아님 — NOT_RESOURCE_OWNER"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 미디어 — RESOURCE_NOT_FOUND")
    })
    @PostMapping("/{mediaRefId}/confirm")
    public ResponseEntity<ApiResponse<String>> confirm(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID mediaRefId) {

        URI servingUrl = confirmUploadUseCase.confirmUpload(principal.userId(), mediaRefId);
        return ResponseEntity.ok(ApiResponse.ok(servingUrl.toString()));
    }

    private void validatePrincipalCanUpload(JwtPrincipal principal, com.memeboo2.haemi.platform.media.domain.MediaType mediaType) {
        boolean guardianType = mediaType == com.memeboo2.haemi.platform.media.domain.MediaType.MEMORY_IMAGE
                || mediaType == com.memeboo2.haemi.platform.media.domain.MediaType.GREETING_VOICE
                || mediaType == com.memeboo2.haemi.platform.media.domain.MediaType.PROFILE_IMAGE;
        if ((guardianType && !principal.isGuardian()) || (!guardianType && !principal.isElder())) {
            throw new DomainException(ErrorCode.ROLE_NOT_ALLOWED);
        }
    }
}
