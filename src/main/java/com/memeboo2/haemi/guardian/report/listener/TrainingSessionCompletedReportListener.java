package com.memeboo2.haemi.guardian.report.listener;

import com.memeboo2.haemi.common.event.CognitiveTrainingCompleted;
import com.memeboo2.haemi.common.persistence.UuidGenerator;
import com.memeboo2.haemi.guardian.report.infrastructure.CognitiveResultSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** CIST 영역별 결과를 guardian/report 전용 읽기 모델에 멱등 투영한다. */
@Component
@RequiredArgsConstructor
public class TrainingSessionCompletedReportListener {

    private final CognitiveResultSnapshotRepository repository;

    @ApplicationModuleListener
    public void on(CognitiveTrainingCompleted event) {
        event.cognitiveAreaResults().forEach(result -> repository.insertIfAbsent(
                UuidGenerator.generate(),
                event.elderId(),
                event.sessionId(),
                event.sessionDate(),
                result.area(),
                result.scoredAnswerCount(),
                result.correctAnswerCount()));
    }
}
