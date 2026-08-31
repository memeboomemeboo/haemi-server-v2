package com.memeboo2.haemi.common.security;

import com.memeboo2.haemi.auth.account.domain.AccountRole;
import com.memeboo2.haemi.auth.api.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InternalBatchEndpointHttpTest {

    @LocalServerPort int port;
    @Autowired JwtTokenProvider jwtTokenProvider;

    @Test
    void 일반_보호자와_어르신_JWT는_내부_배치_API를_실행할_수_없다() throws Exception {
        for (AccountRole role : new AccountRole[]{AccountRole.GUARDIAN, AccountRole.ELDER}) {
            String token = jwtTokenProvider.createAccessToken(UUID.randomUUID(), role);
            assertThat(post("/api/v1/internal/report/dispatch", token).statusCode()).isEqualTo(403);
            assertThat(post("/api/v1/internal/ai/reminiscence/run", token).statusCode()).isEqualTo(403);
        }
    }

    private HttpResponse<String> post(String path, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
