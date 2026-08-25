package com.memeboo2.haemi.elder.training;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memeboo2.haemi.auth.account.domain.AccountRole;
import com.memeboo2.haemi.auth.api.JwtTokenProvider;
import com.memeboo2.haemi.elder.attendance.infrastructure.DailyParticipationRepository;
import com.memeboo2.haemi.elder.training.domain.MaterialSource;
import com.memeboo2.haemi.elder.training.domain.TrainingQuestion;
import com.memeboo2.haemi.elder.training.infrastructure.TrainingAnswerRepository;
import com.memeboo2.haemi.elder.training.infrastructure.TrainingQuestionRepository;
import com.memeboo2.haemi.guardian.eldermanagement.domain.Elder;
import com.memeboo2.haemi.guardian.eldermanagement.domain.ElderRepository;
import com.memeboo2.haemi.platform.content.domain.ContentItem;
import com.memeboo2.haemi.platform.content.infrastructure.ContentItemRepository;
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

/** 실제 JWT·HTTP·JPA·이벤트 리스너를 통과하는 CIST-TRN-001~006 인수 테스트다. */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TrainingSessionHttpTest {

    @LocalServerPort int port;
    @Autowired ElderRepository elderRepository;
    @Autowired ContentItemRepository contentItemRepository;
    @Autowired TrainingQuestionRepository questionRepository;
    @Autowired TrainingAnswerRepository answerRepository;
    @Autowired DailyParticipationRepository participationRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;
    private UUID elderId;
    private String accessToken;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        UUID elderUserId = UUID.randomUUID();
        Elder elder = elderRepository.saveAndFlush(Elder.create(
                elderUserId, UUID.randomUUID(), "API 테스트 어르신", LocalDate.of(1940, 1, 1)));
        elderId = elder.getId();
        accessToken = jwtTokenProvider.createAccessToken(elderUserId, AccountRole.ELDER);
        contentItemRepository.saveAllAndFlush(List.of(
                content("골목 풍경", 1968), content("가족 나들이", 1973), content("추석 저녁", 1978)));
    }

    @Test
    void 세션을_시작해_10개_문항을_순서대로_완료하면_결과와_출석을_만든다() throws Exception {
        JsonNode current = data(post("/api/v1/elder/training/session/enter", null));

        assertThat(current.path("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(current.path("totalQuestionCount").asInt()).isEqualTo(10);
        assertThat(current.path("inactivityReminderSeconds").asInt()).isEqualTo(90);
        assertThat(current.path("currentQuestion").path("questionType").asText()).isEqualTo("ORIENTATION");
        UUID sessionId = UUID.fromString(current.path("id").asText());

        for (int index = 1; index <= 10; index++) {
            JsonNode question = current.path("currentQuestion");
            HttpResponse<String> answered = post(
                    "/api/v1/elder/training/session/current-question/complete", answerPayload(sessionId, question));
            assertThat(answered.statusCode()).isEqualTo(200);
            current = data(answered);
            if (index == 1) {
                assertThat(current.path("feedback").asText()).isEqualTo("맞아요. 다음 문제로 가볼까요?");
            }
        }

        assertThat(current.path("status").asText()).isEqualTo("COMPLETED");
        assertThat(current.path("result").path("sessionId").asText()).isEqualTo(sessionId.toString());
        assertThat(current.path("result").path("delayedRecallSuccessCount").asInt()).isEqualTo(2);
        assertThat(current.toString()).doesNotContain("answerKey", "correct", "accuracy", "score");
        assertThat(answerRepository.findBySessionIdOrderByQuestionNumberAsc(sessionId)).hasSize(10);
        assertThat(participationRepository.existsByElderIdAndParticipationDate(
                elderId, LocalDate.now(com.memeboo2.haemi.common.time.HaemiClock.KST))).isTrue();

        List<TrainingQuestion> questions = questionRepository.findBySessionIdOrderByQuestionNumberAsc(sessionId);
        assertThat(questions).hasSize(10);
        assertThat(questions.subList(3, 6)).extracting(TrainingQuestion::getMaterialSource)
                .containsOnly(MaterialSource.CONTENT);
        assertThat(questions.subList(8, 10)).extracting(TrainingQuestion::getMaterialId)
                .allMatch(questions.subList(3, 6).stream().map(TrainingQuestion::getMaterialId).toList()::contains);

        HttpResponse<String> result = get("/api/v1/elder/training/session/result");
        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(data(result).path("result").path("sessionId").asText()).isEqualTo(sessionId.toString());
    }

    @Test
    void 완료된_문항의_재전송은_다음_문항을_건너뛰지_않는다() throws Exception {
        JsonNode entered = data(post("/api/v1/elder/training/session/enter", null));
        UUID sessionId = UUID.fromString(entered.path("id").asText());
        String firstAnswer = answerPayload(sessionId, entered.path("currentQuestion"));

        assertThat(post("/api/v1/elder/training/session/current-question/complete", firstAnswer).statusCode()).isEqualTo(200);

        HttpResponse<String> duplicate = post("/api/v1/elder/training/session/current-question/complete", firstAnswer);
        assertThat(duplicate.statusCode()).isEqualTo(400);
        assertThat(duplicate.body()).contains("\"code\":\"INVALID_INPUT\"");
        assertThat(data(post("/api/v1/elder/training/session/enter", null))
                .path("currentQuestion").path("questionNumber").asInt()).isEqualTo(2);
    }

    private ContentItem content(String title, int year) {
        return ContentItem.create(
                title,
                "curated/" + title + ".jpg",
                year,
                List.of(title, "추억"),
                "KR",
                60,
                100,
                null);
    }

    private String answerPayload(UUID sessionId, JsonNode question) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("questionId", UUID.fromString(question.path("id").asText()));
        payload.put("questionNumber", question.path("questionNumber").asInt());
        if ("CHOICE".equals(question.path("answerMode").asText())) {
            payload.put("selectedOption", question.path("options").get(0).asText());
        } else {
            payload.put("textAnswer", "사진에 대한 이야기를 들려드려요.");
        }
        return objectMapper.writeValueAsString(payload);
    }

    private JsonNode data(HttpResponse<String> response) throws Exception {
        assertThat(response.statusCode()).isEqualTo(200);
        return objectMapper.readTree(response.body()).path("data");
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest.Builder request = request(path);
        if (body == null) {
            request.POST(HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        }
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return HttpClient.newHttpClient().send(request(path).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + accessToken);
    }
}
