package com.memeboo2.haemi.auth;

import com.memeboo2.haemi.auth.verification.application.RateLimitRowExistsException;
import com.memeboo2.haemi.auth.verification.application.VerificationRateLimitCounter;
import com.memeboo2.haemi.auth.verification.application.VerificationRateLimitCreator;
import com.memeboo2.haemi.auth.verification.infrastructure.VerificationRateLimitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VerificationRateLimitCounterTest {

    private static final String KEY = "email-verification:abc";
    private static final Instant WINDOW = Instant.parse("2026-08-25T00:00:00Z");

    @Mock VerificationRateLimitRepository repository;
    @Mock VerificationRateLimitCreator creator;
    @InjectMocks VerificationRateLimitCounter counter;

    @Test
    void 윈도우의_첫_요청이면_카운터_행을_만들고_1을_반환한다() {
        given(repository.incrementIfPresent(KEY, WINDOW)).willReturn(0);

        assertThat(counter.incrementAndGet(KEY, WINDOW)).isEqualTo(1);

        verify(creator).createFirstAttempt(KEY, WINDOW);
    }

    @Test
    void 이미_있는_윈도우는_원자적_UPDATE로_올린다() {
        given(repository.incrementIfPresent(KEY, WINDOW)).willReturn(1);
        given(repository.findAttemptCount(KEY, WINDOW)).willReturn(4);

        assertThat(counter.incrementAndGet(KEY, WINDOW)).isEqualTo(4);
    }

    @Test
    void 다른_요청이_먼저_행을_만들었으면_증가로_전환한다() {
        given(repository.incrementIfPresent(KEY, WINDOW)).willReturn(0, 1);
        willThrow(new RateLimitRowExistsException(new RuntimeException("duplicate")))
                .given(creator).createFirstAttempt(KEY, WINDOW);
        given(repository.findAttemptCount(KEY, WINDOW)).willReturn(2);

        assertThat(counter.incrementAndGet(KEY, WINDOW)).isEqualTo(2);
    }
}
