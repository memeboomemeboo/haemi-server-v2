package com.memeboo2.haemi.elder.training;

import com.memeboo2.haemi.auth.account.domain.AccountRole;
import com.memeboo2.haemi.auth.api.JwtTokenProvider;
import com.memeboo2.haemi.guardian.eldermanagement.domain.Elder;
import com.memeboo2.haemi.guardian.eldermanagement.domain.ElderRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 실제 JWT 필터·컨트롤러·유스케이스·JPA를 거쳐 CIST 세션 API를 검증한다. */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TrainingSessionHttpTest {

    @LocalServerPort int port;
    @Autowired ElderRepository elderRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;

    private String accessToken;

    @BeforeEach
    void setUp() {
        UUID elderUserId = UUID.randomUUID();
        elderRepository.saveAndFlush(Elder.create(
                elderUserId, UUID.randomUUID(), "API 테스트 어르신", LocalDate.of(1940, 1, 1)));
        accessToken = jwtTokenProvider.createAccessToken(elderUserId, AccountRole.ELDER);
    }

    @Test
    void 인증한_어르신은_세션에_진입하고_현재_문항을_완료할_수_있다() throws Exception {
        HttpResponse<String> entered = send("/api/v1/elder/training/session/enter", null);

        assertThat(entered.statusCode()).isEqualTo(200);
        assertThat(entered.body())
                .contains("\"status\":\"IN_PROGRESS\"")
                .contains("\"currentStep\":\"ORIENTATION\"")
                .contains("\"currentQuestionNumber\":1")
                .contains("\"totalQuestionCount\":10");

        HttpResponse<String> completed = send(
                "/api/v1/elder/training/session/current-question/complete",
                "{\"questionType\":\"ORIENTATION\"}");

        assertThat(completed.statusCode()).isEqualTo(200);
        assertThat(completed.body())
                .contains("\"status\":\"IN_PROGRESS\"")
                .contains("\"currentStep\":\"ORIENTATION\"")
                .contains("\"currentQuestionNumber\":2");

        HttpResponse<String> wrongStep = send(
                "/api/v1/elder/training/session/current-question/complete",
                "{\"questionType\":\"RECALL\"}");

        assertThat(wrongStep.statusCode()).isEqualTo(400);
        assertThat(wrongStep.body()).contains("\"code\":\"INVALID_INPUT\"");
    }

    private HttpResponse<String> send(String path, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + accessToken);
        if (body == null) {
            request.POST(HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
        }
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
