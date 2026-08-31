package com.memeboo2.haemi.platform.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memeboo2.haemi.auth.account.domain.AccountRole;
import com.memeboo2.haemi.auth.api.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/** 공개 미디어 업로드 API의 입력 경계와 확정 키 분리를 검증한다. */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MediaHttpTest {

    @LocalServerPort int port;
    @Autowired JwtTokenProvider jwtTokenProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private String authorization;

    @BeforeEach
    void setUp() {
        authorization = "Bearer " + jwtTokenProvider.createAccessToken(UUID.randomUUID(), AccountRole.GUARDIAN);
    }

    @Test
    void 확정된_이미지는_임시_업로드_URL로_다시_덮어쓸_수_없다() throws Exception {
        HttpResponse<String> requested = postMediaUpload("photo.jpg");
        assertThat(requested.statusCode()).isEqualTo(201);
        JsonNode data = objectMapper.readTree(requested.body()).path("data");
        String mediaRefId = data.path("mediaRefId").asText();
        URI temporaryUploadUrl = localUri(data.path("presignedUrl").asText());

        assertThat(put(temporaryUploadUrl, "12345").statusCode()).isEqualTo(204);
        HttpResponse<String> confirmed = post("/api/v1/media/" + mediaRefId + "/confirm", "");
        assertThat(confirmed.statusCode()).isEqualTo(200);
        URI servingUrl = localUri(objectMapper.readTree(confirmed.body()).path("data").asText());

        // 원래 PUT URL이 아직 호출 가능해도 확정 키가 아닌 임시 키만 바뀐다.
        assertThat(put(temporaryUploadUrl, "67890").statusCode()).isEqualTo(204);
        HttpResponse<String> served = httpClient.send(HttpRequest.newBuilder(servingUrl).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(served.statusCode()).isEqualTo(200);
        assertThat(served.body()).isEqualTo("12345");
    }

    @Test
    void 원본_파일명이_255자를_초과하면_500이_아닌_400을_반환한다() throws Exception {
        HttpResponse<String> response = postMediaUpload("a".repeat(256) + ".jpg");

        assertThat(response.statusCode()).isEqualTo(400);
        JsonNode error = objectMapper.readTree(response.body()).path("error");
        assertThat(error.path("code").asText()).isEqualTo("INVALID_INPUT");
        assertThat(error.path("field").asText()).isEqualTo("originalFilename");
    }

    @Test
    void 같은_해시의_다른_미디어가_먼저_확정되면_409을_반환한다() throws Exception {
        String hash = "a".repeat(64);
        JsonNode first = objectMapper.readTree(postMediaUpload("first.jpg", hash).body()).path("data");
        JsonNode second = objectMapper.readTree(postMediaUpload("second.jpg", hash).body()).path("data");
        assertThat(put(localUri(first.path("presignedUrl").asText()), "12345").statusCode()).isEqualTo(204);
        assertThat(put(localUri(second.path("presignedUrl").asText()), "12345").statusCode()).isEqualTo(204);

        assertThat(post("/api/v1/media/" + first.path("mediaRefId").asText() + "/confirm", "").statusCode())
                .isEqualTo(200);
        HttpResponse<String> conflicted = post("/api/v1/media/" + second.path("mediaRefId").asText() + "/confirm", "");

        assertThat(conflicted.statusCode()).isEqualTo(409);
        assertThat(objectMapper.readTree(conflicted.body()).path("error").path("code").asText())
                .isEqualTo("MEDIA_DUPLICATE_ALREADY_CONFIRMED");
    }

    @Test
    void 같은_해시의_동시_확정은_500_대신_성공과_409으로_수렴한다() throws Exception {
        String hash = "b".repeat(64);
        JsonNode first = objectMapper.readTree(postMediaUpload("first-concurrent.jpg", hash).body()).path("data");
        JsonNode second = objectMapper.readTree(postMediaUpload("second-concurrent.jpg", hash).body()).path("data");
        assertThat(put(localUri(first.path("presignedUrl").asText()), "12345").statusCode()).isEqualTo(204);
        assertThat(put(localUri(second.path("presignedUrl").asText()), "12345").statusCode()).isEqualTo(204);

        CompletableFuture<Integer> firstStatus = CompletableFuture.supplyAsync(
                () -> confirm(first.path("mediaRefId").asText()));
        CompletableFuture<Integer> secondStatus = CompletableFuture.supplyAsync(
                () -> confirm(second.path("mediaRefId").asText()));

        assertThat(java.util.List.of(firstStatus.join(), secondStatus.join()))
                .containsExactlyInAnyOrder(200, 409);
    }

    private HttpResponse<String> postMediaUpload(String originalFilename) throws Exception {
        return postMediaUpload(originalFilename, null);
    }

    private HttpResponse<String> postMediaUpload(String originalFilename, String contentHash) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("mediaType", "MEMORY_IMAGE");
        payload.put("originalFilename", originalFilename);
        payload.put("contentType", "image/jpeg");
        payload.put("declaredSizeBytes", 5);
        payload.put("declaredDurationSeconds", null);
        payload.put("contentHash", contentHash);
        return post("/api/v1/media/upload-request", objectMapper.writeValueAsString(payload));
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", authorization)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private int confirm(String mediaRefId) {
        try {
            return post("/api/v1/media/" + mediaRefId + "/confirm", "").statusCode();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private HttpResponse<String> put(URI uri, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Content-Type", "image/jpeg")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI localUri(String value) {
        return URI.create(value.replace("localhost:8080", "localhost:" + port));
    }
}
