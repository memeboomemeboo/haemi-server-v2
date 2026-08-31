package com.memeboo2.haemi.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** 브라우저 preflight(OPTIONS)가 인증 이전 단계에서 통과하는지 검증한다. */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CorsPreflightHttpTest {

    private static final String CLIENT_ORIGIN = "http://localhost:8100";

    @LocalServerPort int port;

    @Test
    void 인증필요_경로의_preflight는_토큰_없이도_통과한다() throws Exception {
        HttpResponse<String> preflight = preflight("/api/v1/guardian/families", "GET");

        assertThat(preflight.statusCode()).isEqualTo(200);
        assertThat(preflight.headers().firstValue("Access-Control-Allow-Origin"))
                .contains(CLIENT_ORIGIN);
        assertThat(preflight.headers().firstValue("Access-Control-Allow-Credentials"))
                .contains("true");
    }

    @Test
    void 로그인_preflight도_통과한다() throws Exception {
        assertThat(preflight("/api/v1/auth/login", "POST").statusCode()).isEqualTo(200);
    }

    @Test
    void 허용되지_않은_오리진은_거부된다() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/login"))
                .header("Origin", "http://evil.example.com")
                .header("Access-Control-Request-Method", "POST")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
    }

    private HttpResponse<String> preflight(String path, String method) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Origin", CLIENT_ORIGIN)
                .header("Access-Control-Request-Method", method)
                .header("Access-Control-Request-Headers", "authorization,content-type")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
