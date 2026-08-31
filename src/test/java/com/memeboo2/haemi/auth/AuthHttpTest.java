package com.memeboo2.haemi.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memeboo2.haemi.auth.account.domain.AccountRole;
import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
import com.memeboo2.haemi.auth.api.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** AuthController의 회원가입·로그인·토큰재발급·로그아웃·이메일인증 흐름에 대한 HTTP 인수 테스트다. */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthHttpTest {

    @LocalServerPort int port;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @Autowired AccountRepository accountRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String loginId;
    private String authToken;
    private final String password = "password123";

    @BeforeEach
    void setUp() {
        loginId = "test_user_" + UUID.randomUUID().toString().substring(0, 8);
        authToken = jwtTokenProvider.createAccessToken(UUID.randomUUID(), AccountRole.GUARDIAN);
    }

    @Test
    void 보호자_회원가입은_201을_반환한다() throws Exception {
        HttpResponse<String> response = post("/api/v1/auth/guardians/register", registerPayload(loginId));

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode data = objectMapper.readTree(response.body()).path("data");
        assertThat(data.path("userId").asText()).isNotBlank();
    }

    @Test
    void 아이디_중복_확인은_사용가능여부를_반환한다() throws Exception {
        HttpResponse<String> response = get("/api/v1/auth/login-id/availability?loginId=" + loginId);

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode data = objectMapper.readTree(response.body()).path("data");
        assertThat(data.path("loginId").asText()).isEqualTo(loginId);
        assertThat(data.path("available").asBoolean()).isTrue();
    }

    @Test
    void 아이디_중복_확인은_인증없이_호출할_수_있다() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(
                        "http://localhost:" + port + "/api/v1/auth/login-id/availability?loginId=" + loginId))
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode data = objectMapper.readTree(response.body()).path("data");
        assertThat(data.path("loginId").asText()).isEqualTo(loginId);
        assertThat(data.path("available").asBoolean()).isTrue();
    }

    @Test
    void 보호자_회원가입은_선택_이메일과_전화번호도_받는다() throws Exception {
        String phone = "01012345678";
        String email = "optional-" + UUID.randomUUID() + "@example.com";
        Map<String, Object> body = registerPayloadMap(loginId);
        body.put("phone", phone);
        body.put("email", email);

        HttpResponse<String> response = post("/api/v1/auth/guardians/register", objectMapper.writeValueAsString(body));

        assertThat(response.statusCode()).isEqualTo(201);
        UUID userId = UUID.fromString(objectMapper.readTree(response.body()).path("data").path("userId").asText());
        var account = accountRepository.findById(userId).orElseThrow();
        assertThat(account.getPhone()).isEqualTo(phone);
        assertThat(account.getEmail()).isEqualTo(email);
    }

    @Test
    void 중복_이메일로_회원가입하면_409_EMAIL_ALREADY_TAKEN을_반환한다() throws Exception {
        // 테스트 프로필은 Flyway를 끄므로, 운영 V117의 partial unique index를 H2에서 재현한다.
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_accounts_email ON accounts(email)");
        String email = "duplicate-" + UUID.randomUUID() + "@example.com";
        Map<String, Object> first = registerPayloadMap(loginId);
        first.put("email", email);
        Map<String, Object> second = registerPayloadMap("other_" + UUID.randomUUID().toString().substring(0, 8));
        second.put("email", email);

        assertThat(post("/api/v1/auth/guardians/register", objectMapper.writeValueAsString(first)).statusCode())
                .isEqualTo(201);
        HttpResponse<String> response = post("/api/v1/auth/guardians/register", objectMapper.writeValueAsString(second));

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(objectMapper.readTree(response.body()).path("error").path("code").asText())
                .isEqualTo("EMAIL_ALREADY_TAKEN");
    }

    @Test
    void 빈_이메일_문자열로_두_계정을_가입해도_모두_201을_반환한다() throws Exception {
        Map<String, Object> first = registerPayloadMap(loginId);
        first.put("email", "");
        Map<String, Object> second = registerPayloadMap("other_" + UUID.randomUUID().toString().substring(0, 8));
        second.put("email", "");

        assertThat(post("/api/v1/auth/guardians/register", objectMapper.writeValueAsString(first)).statusCode())
                .isEqualTo(201);
        assertThat(post("/api/v1/auth/guardians/register", objectMapper.writeValueAsString(second)).statusCode())
                .isEqualTo(201);
    }

    @Test
    void 이메일_없이_인증ID만_제출하면_400을_반환한다() throws Exception {
        Map<String, Object> body = registerPayloadMap(loginId);
        body.put("emailVerificationId", UUID.randomUUID());

        HttpResponse<String> response = post("/api/v1/auth/guardians/register", objectMapper.writeValueAsString(body));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(objectMapper.readTree(response.body()).path("error").path("code").asText())
                .isEqualTo("INVALID_INPUT");
    }

    @Test
    void 등록후_아이디_중복_확인은_사용불가를_반환한다() throws Exception {
        assertThat(post("/api/v1/auth/guardians/register", registerPayload(loginId)).statusCode()).isEqualTo(201);

        HttpResponse<String> response = get("/api/v1/auth/login-id/availability?loginId=" + loginId);

        JsonNode data = objectMapper.readTree(response.body()).path("data");
        assertThat(data.path("available").asBoolean()).isFalse();
    }

    @Test
    void 잘못된_비밀번호로_로그인하면_401을_반환한다() throws Exception {
        assertThat(post("/api/v1/auth/guardians/register", registerPayload(loginId)).statusCode()).isEqualTo(201);

        HttpResponse<String> response = post("/api/v1/auth/login", loginPayload(loginId, "wrong-password"));

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void 올바른_자격증명으로_로그인하면_토큰을_반환한다() throws Exception {
        assertThat(post("/api/v1/auth/guardians/register", registerPayload(loginId)).statusCode()).isEqualTo(201);

        HttpResponse<String> response = post("/api/v1/auth/login", loginPayload(loginId, password));

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode data = objectMapper.readTree(response.body()).path("data");
        assertThat(data.path("accessToken").asText()).isNotBlank();
        assertThat(data.path("refreshToken").asText()).isNotBlank();
    }

    @Test
    void 빈_pin을_함께_보내도_password_로그인은_200을_반환한다() throws Exception {
        assertThat(post("/api/v1/auth/guardians/register", registerPayload(loginId)).statusCode()).isEqualTo(201);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("loginId", loginId);
        body.put("password", password);
        body.put("pin", "");
        body.put("deviceId", "test-device");

        HttpResponse<String> response = post("/api/v1/auth/login", objectMapper.writeValueAsString(body));

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void 리프레시_토큰으로_새_토큰을_재발급한다() throws Exception {
        assertThat(post("/api/v1/auth/guardians/register", registerPayload(loginId)).statusCode()).isEqualTo(201);
        JsonNode loginData = objectMapper.readTree(
                post("/api/v1/auth/login", loginPayload(loginId, password)).body()).path("data");
        String refreshToken = loginData.path("refreshToken").asText();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("refreshToken", refreshToken);
        body.put("deviceId", "test-device");
        HttpResponse<String> response = post("/api/v1/auth/refresh", objectMapper.writeValueAsString(body));

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode data = objectMapper.readTree(response.body()).path("data");
        assertThat(data.path("accessToken").asText()).isNotBlank();
        assertThat(data.path("refreshToken").asText()).isNotBlank();
    }

    @Test
    void 유효한_토큰으로_로그아웃하면_200을_반환한다() throws Exception {
        assertThat(post("/api/v1/auth/guardians/register", registerPayload(loginId)).statusCode()).isEqualTo(201);
        JsonNode loginData = objectMapper.readTree(
                post("/api/v1/auth/login", loginPayload(loginId, password)).body()).path("data");
        String accessToken = loginData.path("accessToken").asText();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("deviceId", "test-device");
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/logout"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void 유효한_이메일로_인증번호_발송을_요청하면_201을_반환한다() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", "test-" + UUID.randomUUID() + "@example.com");

        HttpResponse<String> response = post("/api/v1/auth/email-verifications", objectMapper.writeValueAsString(body));

        assertThat(response.statusCode()).isEqualTo(201);
    }

    @Test
    void 잘못된_인증번호로_확인하면_400을_반환한다() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", "test-" + UUID.randomUUID() + "@example.com");
        JsonNode verificationId = objectMapper.readTree(
                post("/api/v1/auth/email-verifications", objectMapper.writeValueAsString(body)).body()).path("data");

        Map<String, Object> confirmBody = new LinkedHashMap<>();
        confirmBody.put("code", "000000");
        HttpResponse<String> response = post(
                "/api/v1/auth/email-verifications/" + verificationId.asText() + "/confirm",
                objectMapper.writeValueAsString(confirmBody));

        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void 짧은_비밀번호로_회원가입하면_400을_반환한다() throws Exception {
        Map<String, Object> body = registerPayloadMap(loginId);
        body.put("password", "short");

        HttpResponse<String> response = post("/api/v1/auth/guardians/register", objectMapper.writeValueAsString(body));

        assertThat(response.statusCode()).isEqualTo(400);
    }

    private String registerPayload(String loginId) throws Exception {
        return objectMapper.writeValueAsString(registerPayloadMap(loginId));
    }

    private Map<String, Object> registerPayloadMap(String loginId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "테스트");
        body.put("loginId", loginId);
        body.put("password", password);
        body.put("birthDate", "1990-01-01");
        body.put("pin", "123456");
        return body;
    }

    private String loginPayload(String loginId, String password) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("loginId", loginId);
        body.put("password", password);
        body.put("deviceId", "test-device");
        return objectMapper.writeValueAsString(body);
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + authToken)
                .GET().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
