package com.memeboo2.haemi.platform.content;

import com.memeboo2.haemi.platform.content.domain.ContentExposure;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ContentExposureDomainTest {

    @Test
    void record로_노출_기록을_생성한다() {
        UUID elderId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        Instant now = Instant.now();

        ContentExposure exposure = ContentExposure.record(elderId, contentId, now);

        assertThat(exposure.getId()).isNotNull();
        assertThat(exposure.getElderId()).isEqualTo(elderId);
        assertThat(exposure.getContentId()).isEqualTo(contentId);
        assertThat(exposure.getExposedAt()).isEqualTo(now);
    }

    @Test
    void 같은_콘텐츠를_다른_어르신에게_노출하면_별도_기록이다() {
        UUID contentId = UUID.randomUUID();
        Instant now = Instant.now();

        ContentExposure e1 = ContentExposure.record(UUID.randomUUID(), contentId, now);
        ContentExposure e2 = ContentExposure.record(UUID.randomUUID(), contentId, now);

        assertThat(e1.getId()).isNotEqualTo(e2.getId());
    }

    @Test
    void 같은_어르신에게_다른_콘텐츠를_노출하면_별도_기록이다() {
        UUID elderId = UUID.randomUUID();
        Instant now = Instant.now();

        ContentExposure e1 = ContentExposure.record(elderId, UUID.randomUUID(), now);
        ContentExposure e2 = ContentExposure.record(elderId, UUID.randomUUID(), now);

        assertThat(e1.getId()).isNotEqualTo(e2.getId());
    }
}
