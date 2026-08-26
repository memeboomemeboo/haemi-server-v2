package com.memeboo2.haemi.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CognitiveTrainingCompletedSerializationTest {

    @Test
    void modulith_outbox_255자_열에_영역별_완료이벤트가_들어간다() throws Exception {
        CognitiveTrainingCompleted event = new CognitiveTrainingCompleted(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 8, 26), List.of(
                new CognitiveTrainingCompleted.CognitiveAreaResult("ORIENTATION", 3, 3),
                new CognitiveTrainingCompleted.CognitiveAreaResult("RECALL", 3, 2),
                new CognitiveTrainingCompleted.CognitiveAreaResult("LANGUAGE", 0, 0),
                new CognitiveTrainingCompleted.CognitiveAreaResult("DELAYED_RECALL", 2, 1)
        ));

        String serialized = new ObjectMapper().findAndRegisterModules().writeValueAsString(event);

        assertThat(serialized).hasSizeLessThanOrEqualTo(255);
    }
}
