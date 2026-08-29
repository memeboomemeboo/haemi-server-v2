package com.memeboo2.haemi.elder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memeboo2.haemi.auth.account.domain.Account;
import com.memeboo2.haemi.auth.account.domain.AccountRole;
import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
import com.memeboo2.haemi.auth.api.JwtTokenProvider;
import com.memeboo2.haemi.elder.response.domain.Response;
import com.memeboo2.haemi.elder.response.infrastructure.ResponseRepository;
import com.memeboo2.haemi.guardian.dailycare.domain.DailyCare;
import com.memeboo2.haemi.guardian.dailycare.infrastructure.DailyCareRepository;
import com.memeboo2.haemi.guardian.eldermanagement.domain.Elder;
import com.memeboo2.haemi.guardian.eldermanagement.domain.ElderRepository;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLink;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLinkRepository;
import com.memeboo2.haemi.guardian.family.domain.Family;
import com.memeboo2.haemi.guardian.family.domain.FamilyRepository;
import com.memeboo2.haemi.guardian.memory.domain.Memory;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
import com.memeboo2.haemi.platform.media.domain.MediaRef;
import com.memeboo2.haemi.platform.media.domain.MediaType;
import com.memeboo2.haemi.platform.media.infrastructure.LocalObjectStorage;
import com.memeboo2.haemi.platform.media.infrastructure.MediaRefRepository;
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
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ElderHttpTest {

    @LocalServerPort int port;
    @Autowired AccountRepository accountRepository;
    @Autowired FamilyRepository familyRepository;
    @Autowired ElderRepository elderRepository;
    @Autowired GuardianElderLinkRepository linkRepository;
    @Autowired MemoryRepository memoryRepository;
    @Autowired DailyCareRepository dailyCareRepository;
    @Autowired ResponseRepository responseRepository;
    @Autowired MediaRefRepository mediaRefRepository;
    @Autowired LocalObjectStorage localObjectStorage;
    @Autowired JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID elderUserId;
    private UUID elderId;
    private UUID guardianId;
    private String elderToken;

    @BeforeEach
    void setUp() {
        // Guardian account & family
        Account guardianAccount = accountRepository.saveAndFlush(
                Account.guardian("보호자", "guardian_" + UUID.randomUUID().toString().substring(0, 8),
                        "$2a$10$dummyHashForTest", "1990-01-01", null, null, null));
        guardianId = guardianAccount.getId();

        Family family = Family.create("테스트 가족", UUID.randomUUID().toString().substring(0, 8));
        family.addMember(guardianId);
        family = familyRepository.saveAndFlush(family);

        // Elder account & entity
        Account elderAccount = accountRepository.saveAndFlush(
                Account.elder("테스트 어르신", "elder_" + UUID.randomUUID().toString().substring(0, 8),
                        "$2a$10$dummyHashForTest", "1945-03-15", null, null));
        elderUserId = elderAccount.getId();

        Elder elder = elderRepository.saveAndFlush(
                Elder.create(elderUserId, family.getId(), "테스트 어르신", LocalDate.of(1945, 3, 15)));
        elderId = elder.getId();

        linkRepository.saveAndFlush(GuardianElderLink.create(guardianId, elderId));

        elderToken = jwtTokenProvider.createAccessToken(elderUserId, AccountRole.ELDER);
    }

    @Test
    void 어르신_홈_화면을_조회한다() throws Exception {
        HttpResponse<String> response = get("/api/v1/elder/home");
        assertThat(response.statusCode())
                .as("status=%d body=%s", response.statusCode(), response.body())
                .isEqualTo(200);
    }

    @Test
    void 어르신_추억_목록을_조회한다() throws Exception {
        memoryRepository.saveAndFlush(Memory.create(elderId, "추억1", null, "한마디", 2020));

        HttpResponse<String> response = get("/api/v1/elder/memories");
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode data = data(response);
        assertThat(data.isArray()).isTrue();
    }

    @Test
    void 어르신_추억_상세를_조회한다() throws Exception {
        Memory memory = memoryRepository.saveAndFlush(Memory.create(elderId, "추억상세", null, "한마디", 2020));

        HttpResponse<String> response = get("/api/v1/elder/memories/" + memory.getId());
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode data = data(response);
        assertThat(data.path("title").asText()).isEqualTo("추억상세");
    }

    // mark-viewed는 네이티브 ON CONFLICT 쿼리를 사용해 H2에서 실행 불가 — PostgreSQL 통합 테스트에서 검증

    @Test
    void 어르신_수신함을_조회한다() throws Exception {
        dailyCareRepository.saveAndFlush(
                DailyCare.text(guardianId, elderId,
                        LocalDate.now(com.memeboo2.haemi.common.time.HaemiClock.KST),
                        "오늘 하루도 건강하세요", 30));

        HttpResponse<String> response = get("/api/v1/elder/inbox");
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode data = data(response);
        assertThat(data.isArray()).isTrue();
    }

    @Test
    void 어르신_수신함_읽음_처리() throws Exception {
        DailyCare care = dailyCareRepository.saveAndFlush(
                DailyCare.text(guardianId, elderId,
                        LocalDate.now(com.memeboo2.haemi.common.time.HaemiClock.KST),
                        "읽음처리 테스트", 30));

        HttpResponse<String> response = post("/api/v1/elder/inbox/" + care.getId() + "/read", null);
        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void 어르신_마음전하기_감정응답() throws Exception {
        Memory memory = memoryRepository.saveAndFlush(Memory.create(elderId, "감정추억", null, "한마디", 2020));

        String body = objectMapper.writeValueAsString(Map.of("emotions", List.of("LOVE")));
        HttpResponse<String> response = post(
                "/api/v1/elder/memories/" + memory.getId() + "/responses/emotion", body);
        assertThat(response.statusCode()).isEqualTo(201);
    }

    @Test
    void 어르신_텍스트_댓글() throws Exception {
        Memory memory = memoryRepository.saveAndFlush(Memory.create(elderId, "댓글추억", null, "한마디", 2020));

        String body = objectMapper.writeValueAsString(Map.of("text", "예쁜 사진이네요"));
        HttpResponse<String> response = post(
                "/api/v1/elder/memories/" + memory.getId() + "/responses/text", body);
        assertThat(response.statusCode()).isEqualTo(201);
    }

    @Test
    void 음성_답변은_전사_실패_상태를_조회할_수_있다() throws Exception {
        Memory memory = memoryRepository.saveAndFlush(Memory.create(elderId, "음성추억", null, "한마디", 2020));
        MediaRef media = mediaRefRepository.saveAndFlush(MediaRef.pending(
                MediaType.RESPONSE_VOICE,
                "response_voice/" + UUID.randomUUID() + ".aac",
                "voice.aac", "audio/aac", 3, 12, elderUserId,
                Instant.now().plusSeconds(300), Instant.now().plusSeconds(86_400), null));
        localObjectStorage.put(media.getStorageKey(), "audio/aac", new byte[]{1, 2, 3}, 12);

        HttpResponse<String> created = post(
                "/api/v1/elder/memories/" + memory.getId() + "/responses/voice",
                objectMapper.writeValueAsString(Map.of("mediaRefId", media.getId())));

        assertThat(created.statusCode()).as("body=%s", created.body()).isEqualTo(201);
        UUID responseId = UUID.fromString(data(created).asText());
        Response saved = awaitTranscriptStatus(responseId, "FAILED");
        assertThat(saved.getTranscript()).isNull();
        assertThat(saved.getTranscriptStatus().name()).isEqualTo("FAILED");

        HttpResponse<String> listed = get("/api/v1/elder/memories/" + memory.getId() + "/responses");
        assertThat(listed.statusCode()).isEqualTo(200);
        JsonNode item = data(listed).get(0);
        assertThat(item.path("transcriptionStatus").asText()).isEqualTo("FAILED");
        assertThat(item.path("transcript").isNull()).isTrue();
    }

    @Test
    void 어르신_답변_목록_조회() throws Exception {
        Memory memory = memoryRepository.saveAndFlush(Memory.create(elderId, "답변추억", null, "한마디", 2020));

        // POST로 답변을 생성해야 트랜잭션 안에서 emotions이 초기화된다
        String body = objectMapper.writeValueAsString(Map.of("text", "답변입니다"));
        post("/api/v1/elder/memories/" + memory.getId() + "/responses/text", body);

        HttpResponse<String> response = get(
                "/api/v1/elder/memories/" + memory.getId() + "/responses");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(data(response).isArray()).isTrue();
    }

    @Test
    void 오늘의_회상_콘텐츠를_조회한다() throws Exception {
        HttpResponse<String> response = get("/api/v1/elder/reminiscence/today");
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode data = data(response);
        assertThat(data.has("date")).isTrue();
        assertThat(data.has("content")).isTrue();
    }

    @Test
    void 인증없이_어르신_API에_접근하면_401() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/elder/home"))
                .GET().build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void 보호자_토큰으로_어르신_API에_접근하면_거부된다() throws Exception {
        String guardianToken = jwtTokenProvider.createAccessToken(guardianId, AccountRole.GUARDIAN);
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/elder/home"))
                .header("Authorization", "Bearer " + guardianToken).GET().build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isIn(401, 403);
    }

    private JsonNode data(HttpResponse<String> response) throws Exception {
        return objectMapper.readTree(response.body()).path("data");
    }

    private Response awaitTranscriptStatus(UUID responseId, String expectedStatus) throws InterruptedException {
        Response latest = null;
        for (int attempt = 0; attempt < 40; attempt++) {
            latest = responseRepository.findById(responseId).orElseThrow();
            if (expectedStatus.equals(latest.getTranscriptStatus().name())) {
                return latest;
            }
            Thread.sleep(50);
        }
        return latest;
    }

    private HttpResponse<String> get(String path) throws Exception {
        return HttpClient.newHttpClient().send(
                request(path).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest.Builder req = request(path);
        if (body == null) {
            req.POST(HttpRequest.BodyPublishers.noBody());
        } else {
            req.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
        }
        return HttpClient.newHttpClient().send(req.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + elderToken);
    }
}
