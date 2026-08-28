package com.memeboo2.haemi.guardian;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memeboo2.haemi.auth.account.domain.Account;
import com.memeboo2.haemi.auth.account.domain.AccountRole;
import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
import com.memeboo2.haemi.auth.api.JwtTokenProvider;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.dailycare.infrastructure.DailyCareRepository;
import com.memeboo2.haemi.guardian.eldermanagement.domain.Elder;
import com.memeboo2.haemi.guardian.eldermanagement.domain.ElderRepository;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLink;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLinkRepository;
import com.memeboo2.haemi.guardian.family.domain.Family;
import com.memeboo2.haemi.guardian.family.domain.FamilyRepository;
import com.memeboo2.haemi.guardian.memory.domain.Memory;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** guardian 하위 컨트롤러(가족·어르신·프로필·링크·추억·하루한마디·홈·리포트)의 실제 HTTP 인수 테스트다. */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GuardianHttpTest {

    @LocalServerPort int port;
    @Autowired AccountRepository accountRepository;
    @Autowired FamilyRepository familyRepository;
    @Autowired ElderRepository elderRepository;
    @Autowired GuardianElderLinkRepository linkRepository;
    @Autowired MemoryRepository memoryRepository;
    @Autowired DailyCareRepository dailyCareRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID guardianId;
    private String accessToken;

    @BeforeEach
    void setUp() {
        guardianId = createGuardianAccount("보호자1", "guardian1_" + UUID.randomUUID());
        accessToken = jwtTokenProvider.createAccessToken(guardianId, AccountRole.GUARDIAN);
    }

    // ---------- FamilyController ----------

    @Test
    void 가족을_생성하면_초대코드와_함께_201을_반환한다() throws Exception {
        HttpResponse<String> response = post("/api/v1/guardian/families",
                objectMapper.writeValueAsString(Map.of("name", "우리가족", "memo", "메모")));

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode data = data(response, 201);
        assertThat(data.path("familyId").asText()).isNotBlank();
        assertThat(data.path("inviteCode").asText()).isNotBlank();
    }

    @Test
    void 가족_생성_시_이름이_비어있으면_400이다() throws Exception {
        HttpResponse<String> response = post("/api/v1/guardian/families",
                objectMapper.writeValueAsString(Map.of("name", "")));

        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void 초대코드로_가족에_합류하면_204를_반환한다() throws Exception {
        Family family = familyRepository.saveAndFlush(Family.create("초대가족", "INVITE0001"));
        UUID joiner = createGuardianAccount("합류자", "joiner_" + UUID.randomUUID());
        String joinerToken = jwtTokenProvider.createAccessToken(joiner, AccountRole.GUARDIAN);

        HttpResponse<String> response = post("/api/v1/guardian/families/join",
                objectMapper.writeValueAsString(Map.of("inviteCode", family.getInviteCode())), joinerToken);

        assertThat(response.statusCode()).isEqualTo(204);
    }

    @Test
    void 존재하지_않는_초대코드는_404를_반환한다() throws Exception {
        HttpResponse<String> response = post("/api/v1/guardian/families/join",
                objectMapper.writeValueAsString(Map.of("inviteCode", "NOTEXIST01")));

        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void 소속_가족이_없으면_내_가족_조회는_data_null을_반환한다() throws Exception {
        HttpResponse<String> response = get("/api/v1/guardian/families/my");

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.path("data").isNull() || json.path("data").isMissingNode()).isTrue();
    }

    @Test
    void 내_가족_조회는_생성한_가족_정보를_반환한다() throws Exception {
        JsonNode created = data(post("/api/v1/guardian/families",
                objectMapper.writeValueAsString(Map.of("name", "조회가족"))), 201);

        HttpResponse<String> response = get("/api/v1/guardian/families/my");
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode data = objectMapper.readTree(response.body()).path("data");
        assertThat(data.path("name").asText()).isEqualTo("조회가족");
    }

    @Test
    void 인증토큰이_없으면_401을_반환한다() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/v1/guardian/families/my"))
                .GET().build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
    }

    // ---------- ElderController ----------

    @Test
    void 어르신을_등록하면_201과_어르신_ID를_반환한다() throws Exception {
        JsonNode family = data(post("/api/v1/guardian/families",
                objectMapper.writeValueAsString(Map.of("name", "어르신가족"))), 201);
        UUID familyId = UUID.fromString(family.path("familyId").asText());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("familyId", familyId);
        body.put("name", "김어르신");
        body.put("birthDate", "1945-05-05");
        body.put("loginId", "elder_" + UUID.randomUUID().toString().substring(0, 8));
        body.put("pin", "123456");
        body.put("phone", "010-1111-2222");
        body.put("gender", "남");

        HttpResponse<String> response = post("/api/v1/guardian/elders", objectMapper.writeValueAsString(body));

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(data(response, 201).asText()).isNotBlank();

        Map<String, Object> login = new LinkedHashMap<>();
        login.put("loginId", body.get("loginId"));
        login.put("pin", "123456");
        login.put("deviceId", "elder-registration-test");
        HttpResponse<String> loginResponse = post("/api/v1/auth/login", objectMapper.writeValueAsString(login));
        assertThat(loginResponse.statusCode()).isEqualTo(200);
    }

    @Test
    void 어르신_등록_시_필수값_누락은_400이다() throws Exception {
        HttpResponse<String> response = post("/api/v1/guardian/elders",
                objectMapper.writeValueAsString(Map.of("name", "")));

        assertThat(response.statusCode()).isEqualTo(400);
    }

    // ---------- ProfileController ----------

    @Test
    void 보호자_프로필을_조회한다() throws Exception {
        HttpResponse<String> response = get("/api/v1/guardian/profile");

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode data = data(response, 200);
        assertThat(data.path("userId").asText()).isEqualTo(guardianId.toString());
        assertThat(data.path("name").asText()).isEqualTo("보호자1");
    }

    @Test
    void 보호자_프로필을_수정한다() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("loginId", "newlogin_" + UUID.randomUUID().toString().substring(0, 6));

        HttpResponse<String> response = patch("/api/v1/guardian/profile", objectMapper.writeValueAsString(body));

        assertThat(response.statusCode()).isEqualTo(200);
    }

    // ---------- LinkController ----------

    @Test
    void 링크된_어르신의_역할을_변경한다() throws Exception {
        UUID elderUserId = UUID.randomUUID();
        Family family = familyRepository.saveAndFlush(Family.create("링크가족", "LINKCODE01"));
        family.addMember(guardianId);
        familyRepository.saveAndFlush(family);
        Elder elder = elderRepository.saveAndFlush(
                Elder.create(elderUserId, family.getId(), "링크어르신", LocalDate.of(1950, 1, 1)));
        linkRepository.saveAndFlush(GuardianElderLink.create(guardianId, elder.getId()));

        HttpResponse<String> response = patch(
                "/api/v1/guardian/elders/" + elder.getId() + "/link/role",
                objectMapper.writeValueAsString(Map.of("role", "DAUGHTER")));

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(linkRepository.findByGuardianIdAndElderId(guardianId, elder.getId()).orElseThrow().getRole())
                .isEqualTo(GuardianRole.DAUGHTER);
    }

    @Test
    void 링크되지_않은_어르신의_역할변경은_403이다() throws Exception {
        HttpResponse<String> response = patch(
                "/api/v1/guardian/elders/" + UUID.randomUUID() + "/link/role",
                objectMapper.writeValueAsString(Map.of("role", "SON")));

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void 링크_해제는_204를_반환한다() throws Exception {
        UUID elderUserId = UUID.randomUUID();
        Family family = familyRepository.saveAndFlush(Family.create("해제가족", "UNLINK0001"));
        family.addMember(guardianId);
        UUID otherGuardianId = createGuardianAccount("보호자2", "guardian2_" + UUID.randomUUID());
        family.addMember(otherGuardianId);
        familyRepository.saveAndFlush(family);
        Elder elder = elderRepository.saveAndFlush(
                Elder.create(elderUserId, family.getId(), "해제어르신", LocalDate.of(1950, 1, 1)));
        linkRepository.saveAndFlush(GuardianElderLink.create(guardianId, elder.getId()));
        linkRepository.saveAndFlush(GuardianElderLink.create(otherGuardianId, elder.getId()));

        HttpResponse<String> response = delete("/api/v1/guardian/elders/" + elder.getId() + "/link");

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(linkRepository.findByGuardianIdAndElderId(guardianId, elder.getId())).isEmpty();
    }

    // ---------- MemoryController ----------

    @Test
    void 추억을_등록하고_목록_상세_수정_삭제까지_수행한다() throws Exception {
        UUID elderId = createLinkedElder();

        Map<String, Object> registerBody = new LinkedHashMap<>();
        registerBody.put("elderId", elderId);
        registerBody.put("title", "제목");
        registerBody.put("memo", null);
        registerBody.put("message", "한마디");
        registerBody.put("memoryYear", 2020);
        registerBody.put("memoryMonth", 4);
        registerBody.put("place", "구지면");
        registerBody.put("mediaRefIds", List.of());

        HttpResponse<String> registerResponse = post("/api/v1/guardian/memories",
                objectMapper.writeValueAsString(registerBody));
        assertThat(registerResponse.statusCode()).isEqualTo(201);
        UUID memoryId = UUID.fromString(data(registerResponse, 201).asText());

        HttpResponse<String> listResponse = get("/api/v1/guardian/memories?elderId=" + elderId);
        assertThat(listResponse.statusCode()).isEqualTo(200);
        assertThat(data(listResponse, 200).isArray()).isTrue();
        assertThat(data(listResponse, 200)).hasSize(1);
        assertThat(data(listResponse, 200).get(0).path("place").asText()).isEqualTo("구지면");
        assertThat(data(listResponse, 200).get(0).path("memoryYear").asInt()).isEqualTo(2020);
        assertThat(data(listResponse, 200).get(0).path("memoryMonth").asInt()).isEqualTo(4);

        HttpResponse<String> allListResponse = get("/api/v1/guardian/memories");
        assertThat(allListResponse.statusCode()).isEqualTo(200);
        assertThat(data(allListResponse, 200)).hasSize(1);
        assertThat(data(allListResponse, 200).get(0).path("elderId").asText()).isEqualTo(elderId.toString());

        HttpResponse<String> detailResponse = get("/api/v1/guardian/memories/" + memoryId);
        assertThat(detailResponse.statusCode()).isEqualTo(200);
        assertThat(data(detailResponse, 200).path("title").asText()).isEqualTo("제목");
        assertThat(data(detailResponse, 200).path("place").asText()).isEqualTo("구지면");
        assertThat(data(detailResponse, 200).path("memoryMonth").asInt()).isEqualTo(4);

        Map<String, Object> updateBody = new LinkedHashMap<>();
        updateBody.put("title", "수정된제목");
        updateBody.put("memo", null);
        updateBody.put("message", "수정된한마디");
        updateBody.put("memoryYear", 2021);
        updateBody.put("memoryMonth", 5);
        updateBody.put("place", "대구");
        updateBody.put("mediaRefIds", List.of());
        HttpResponse<String> updateResponse = put("/api/v1/guardian/memories/" + memoryId,
                objectMapper.writeValueAsString(updateBody));
        assertThat(updateResponse.statusCode()).isEqualTo(204);

        HttpResponse<String> afterUpdate = get("/api/v1/guardian/memories/" + memoryId);
        assertThat(data(afterUpdate, 200).path("title").asText()).isEqualTo("수정된제목");
        assertThat(data(afterUpdate, 200).path("place").asText()).isEqualTo("대구");
        assertThat(data(afterUpdate, 200).path("memoryMonth").asInt()).isEqualTo(5);

        HttpResponse<String> deleteResponse = delete("/api/v1/guardian/memories/" + memoryId);
        assertThat(deleteResponse.statusCode()).isEqualTo(204);

        HttpResponse<String> afterDelete = get("/api/v1/guardian/memories/" + memoryId);
        assertThat(afterDelete.statusCode()).isEqualTo(404);
    }

    @Test
    void 링크되지_않은_어르신의_추억등록은_403이다() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("elderId", UUID.randomUUID());
        body.put("title", "제목");
        body.put("message", "한마디");
        body.put("memoryYear", 2020);
        body.put("mediaRefIds", List.of());

        HttpResponse<String> response = post("/api/v1/guardian/memories", objectMapper.writeValueAsString(body));

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void 다른_보호자가_작성한_추억_수정은_403이다() throws Exception {
        UUID elderId = createLinkedElder();
        Memory memory = memoryRepository.saveAndFlush(Memory.create(elderId, "제목", null, "메시지", 2020));

        UUID otherGuardianId = createGuardianAccount("다른보호자", "other_" + UUID.randomUUID());
        linkRepository.saveAndFlush(GuardianElderLink.create(otherGuardianId, elderId));
        String otherToken = jwtTokenProvider.createAccessToken(otherGuardianId, AccountRole.GUARDIAN);

        Map<String, Object> updateBody = new LinkedHashMap<>();
        updateBody.put("title", "수정시도");
        updateBody.put("message", "수정메시지");
        updateBody.put("memoryYear", 2021);
        updateBody.put("mediaRefIds", List.of());

        HttpResponse<String> response = put("/api/v1/guardian/memories/" + memory.getId(),
                objectMapper.writeValueAsString(updateBody), otherToken);

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void 존재하지_않는_추억_조회는_404이다() throws Exception {
        HttpResponse<String> response = get("/api/v1/guardian/memories/" + UUID.randomUUID());

        assertThat(response.statusCode()).isEqualTo(404);
    }

    // ---------- DailyCareController ----------

    @Test
    void 하루한마디_텍스트를_전송하고_보낸이력을_조회한다() throws Exception {
        UUID elderId = createLinkedElder();

        HttpResponse<String> sendResponse = post(
                "/api/v1/guardian/elders/" + elderId + "/daily-care/text",
                objectMapper.writeValueAsString(Map.of("text", "오늘 하루도 건강하세요")));
        assertThat(sendResponse.statusCode()).isEqualTo(201);
        assertThat(data(sendResponse, 201).asText()).isNotBlank();

        HttpResponse<String> historyResponse = get("/api/v1/guardian/elders/" + elderId + "/daily-care/sent");
        assertThat(historyResponse.statusCode()).isEqualTo(200);
        assertThat(data(historyResponse, 200)).hasSize(1);
    }

    @Test
    void 하루한마디_텍스트가_길이제한을_넘으면_400이다() throws Exception {
        UUID elderId = createLinkedElder();

        HttpResponse<String> response = post(
                "/api/v1/guardian/elders/" + elderId + "/daily-care/text",
                objectMapper.writeValueAsString(Map.of("text", "가".repeat(101))));

        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void 링크되지_않은_어르신에게_하루한마디_전송은_403이다() throws Exception {
        HttpResponse<String> response = post(
                "/api/v1/guardian/elders/" + UUID.randomUUID() + "/daily-care/text",
                objectMapper.writeValueAsString(Map.of("text", "안녕하세요")));

        assertThat(response.statusCode()).isEqualTo(403);
    }

    // ---------- GuardianHomeController ----------

    @Test
    void 보호자_홈_화면을_조회한다() throws Exception {
        createLinkedElder();

        HttpResponse<String> response = get("/api/v1/guardian/home");

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode elder = data(response, 200).path("elders").get(0);
        assertThat(elder.has("condition")).isTrue();
        assertThat(elder.has("todayCondition")).isFalse();
    }

    // ---------- ReportController ----------

    @Test
    void 어르신_리포트_목록을_조회한다() throws Exception {
        createLinkedElder();

        HttpResponse<String> response = get("/api/v1/guardian/report/elders");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(data(response, 200).isArray()).isTrue();
    }

    @Test
    void 어르신_요약_리포트를_조회한다() throws Exception {
        UUID elderId = createLinkedElder();

        HttpResponse<String> response = get("/api/v1/guardian/elders/" + elderId + "/report/summary");

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void 출석_리포트를_조회한다() throws Exception {
        UUID elderId = createLinkedElder();

        HttpResponse<String> response = get("/api/v1/guardian/elders/" + elderId + "/report/attendance");

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void 링크되지_않은_어르신의_리포트_요약은_403이다() throws Exception {
        HttpResponse<String> response = get("/api/v1/guardian/elders/" + UUID.randomUUID() + "/report/summary");

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void 인지_영역별_상태를_조회한다() throws Exception {
        UUID elderId = createLinkedElder();

        HttpResponse<String> response = get("/api/v1/guardian/elders/" + elderId + "/report/cognitive-status");

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void 이번_주_하이라이트를_조회한다() throws Exception {
        UUID elderId = createLinkedElder();

        HttpResponse<String> response = get("/api/v1/guardian/elders/" + elderId + "/report/highlight");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(data(response, 200).path("items").isArray()).isTrue();
    }

    @Test
    void 이번_주_하이라이트를_수정한다() throws Exception {
        UUID elderId = createLinkedElder();

        HttpResponse<String> response = patch("/api/v1/guardian/elders/" + elderId + "/report/highlight",
                objectMapper.writeValueAsString(Map.of("items", List.of(Map.of(
                        "title", "이번 주 하이라이트", "body", "좋은 하루")))));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("items", "이번 주 하이라이트", "좋은 하루");
    }

    @Test
    void 오늘의_기록은_날짜와_디자인_타임라인_형태를_반환한다() throws Exception {
        UUID elderId = createLinkedElder();

        HttpResponse<String> response = get("/api/v1/guardian/elders/" + elderId + "/activities");

        JsonNode timeline = data(response, 200);
        assertThat(timeline.path("date").asText()).matches("\\d{4}-\\d{2}-\\d{2}");
        assertThat(timeline.path("items").isArray()).isTrue();
    }

    @Test
    void 오늘의_기록의_잘못된_날짜는_400이다() throws Exception {
        UUID elderId = createLinkedElder();

        HttpResponse<String> response = get("/api/v1/guardian/elders/" + elderId + "/activities?date=2026-99-99");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(objectMapper.readTree(response.body()).path("error").path("code").asText())
                .isEqualTo("INVALID_INPUT");
    }

    @Test
    void 서포트_가이드를_조회한다() throws Exception {
        UUID elderId = createLinkedElder();

        HttpResponse<String> response = get("/api/v1/guardian/elders/" + elderId + "/report/support-guide");

        assertThat(response.statusCode()).isEqualTo(200);
    }

    // ---------- PdfReportController ----------

    @Test
    void 리포트_PDF를_다운로드한다() throws Exception {
        UUID elderId = createLinkedElder();

        HttpResponse<String> response = get("/api/v1/guardian/elders/" + elderId + "/report/pdf");

        assertThat(response.statusCode()).isEqualTo(200);
    }

    // ---------- GuardianActivityController ----------

    @Test
    void 오늘의_기록_타임라인을_조회한다() throws Exception {
        UUID elderId = createLinkedElder();

        HttpResponse<String> response = get("/api/v1/guardian/elders/" + elderId + "/activities");

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void 날짜를_지정해서_오늘의_기록_타임라인을_조회한다() throws Exception {
        UUID elderId = createLinkedElder();

        HttpResponse<String> response = get("/api/v1/guardian/elders/" + elderId + "/activities?date=2025-01-01");

        assertThat(response.statusCode()).isEqualTo(200);
    }

    // ---------- DailyCareController voice ----------

    @Test
    void 존재하지_않는_음성_미디어로_하루한마디를_전송하면_404를_반환한다() throws Exception {
        UUID elderId = createLinkedElder();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mediaRefId", UUID.randomUUID());
        body.put("durationSeconds", 5);

        HttpResponse<String> response = post(
                "/api/v1/guardian/elders/" + elderId + "/daily-care/voice",
                objectMapper.writeValueAsString(body));

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(objectMapper.readTree(response.body()).path("error").path("code").asText())
                .isEqualTo("RESOURCE_NOT_FOUND");
    }

    // ---------- helpers ----------

    private UUID createLinkedElder() {
        UUID elderUserId = UUID.randomUUID();
        Family family = familyRepository.saveAndFlush(Family.create("가족" + UUID.randomUUID(), "CODE" + UUID.randomUUID().toString().substring(0, 6)));
        family.addMember(guardianId);
        familyRepository.saveAndFlush(family);
        Elder elder = elderRepository.saveAndFlush(
                Elder.create(elderUserId, family.getId(), "테스트어르신", LocalDate.of(1945, 3, 3)));
        linkRepository.saveAndFlush(GuardianElderLink.create(guardianId, elder.getId()));
        return elder.getId();
    }

    private UUID createGuardianAccount(String name, String loginId) {
        Account account = Account.guardian(
                name, loginId, "hashed-password", "1980-01-01", "010-0000-0000",
                loginId + "@example.com", "hashed-pin");
        return accountRepository.saveAndFlush(account).getId();
    }

    private JsonNode data(HttpResponse<String> response, int expectedStatus) throws Exception {
        assertThat(response.statusCode()).isEqualTo(expectedStatus);
        return objectMapper.readTree(response.body()).path("data");
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        return post(path, body, accessToken);
    }

    private HttpResponse<String> post(String path, String body, String token) throws Exception {
        HttpRequest.Builder request = request(path, token);
        if (body == null) {
            request.POST(HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        }
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> put(String path, String body) throws Exception {
        return put(path, body, accessToken);
    }

    private HttpResponse<String> put(String path, String body, String token) throws Exception {
        HttpRequest request = request(path, token)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> patch(String path, String body) throws Exception {
        HttpRequest request = request(path, accessToken)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> delete(String path) throws Exception {
        HttpRequest request = request(path, accessToken).DELETE().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return HttpClient.newHttpClient().send(request(path, accessToken).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder request(String path, String token) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token);
    }
}
