package com.memeboo2.haemi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** 실제 Spring 컨텍스트와 HTTP 경로가 함께 기동하는지 검증한다. */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationStartupHttpTest {

    @LocalServerPort int port;

    @Test
    void health_보안_입력오류와_개발용_스토리지_경로가_실제로_동작한다() throws Exception {
        assertThat(send("/actuator/health", "GET", null).statusCode()).isEqualTo(200);

        HttpResponse<String> invalidEmail = send("/api/v1/auth/email-verifications", "POST", "{\"email\":\"invalid\"}");
        assertThat(invalidEmail.statusCode()).isEqualTo(400);
        assertThat(invalidEmail.body()).contains("INVALID_INPUT");

        assertThat(send("/api/v1/guardian/families", "GET", null).statusCode()).isEqualTo(401);

        HttpResponse<String> upload = send("/internal/storage/upload?key=http-test-object&contentType=text/plain", "PUT", "hello");
        assertThat(upload.statusCode()).isEqualTo(204);

        HttpResponse<String> serve = send("/internal/storage/serve?key=http-test-object", "GET", null);
        assertThat(serve.statusCode()).isEqualTo(200);
        assertThat(serve.body()).isEqualTo("hello");
    }

    private HttpResponse<String> send(String path, String method, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", path.contains("storage") ? "text/plain" : "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
