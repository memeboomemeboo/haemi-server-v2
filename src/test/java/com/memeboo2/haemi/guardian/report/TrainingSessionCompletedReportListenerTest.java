package com.memeboo2.haemi.guardian.report;

import com.memeboo2.haemi.common.event.CognitiveTrainingCompleted;
import com.memeboo2.haemi.guardian.report.infrastructure.CognitiveResultSnapshotRepository;
import com.memeboo2.haemi.guardian.report.listener.TrainingSessionCompletedReportListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrainingSessionCompletedReportListenerTest {

    @Mock CognitiveResultSnapshotRepository repository;
    @InjectMocks TrainingSessionCompletedReportListener listener;

    @Test
    void 완료이벤트의_영역별_집계를_리포트_스냅샷으로_원자적_적재한다() {
        UUID elderId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        LocalDate sessionDate = LocalDate.of(2026, 8, 26);
        CognitiveTrainingCompleted event = new CognitiveTrainingCompleted(elderId, sessionId, sessionDate, List.of(
                new CognitiveTrainingCompleted.CognitiveAreaResult("ORIENTATION", 3, 2),
                new CognitiveTrainingCompleted.CognitiveAreaResult("LANGUAGE", 0, 0)
        ));

        listener.on(event);

        ArgumentCaptor<String> area = ArgumentCaptor.forClass(String.class);
        verify(repository, org.mockito.Mockito.times(2)).insertIfAbsent(
                any(), org.mockito.Mockito.eq(elderId), org.mockito.Mockito.eq(sessionId),
                org.mockito.Mockito.eq(sessionDate), area.capture(), any(Integer.class), any(Integer.class));
        assertThat(area.getAllValues()).containsExactly("ORIENTATION", "LANGUAGE");
    }
}
