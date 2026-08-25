package com.memeboo2.haemi.guardian.report.listener;

import com.memeboo2.haemi.common.event.AttendanceRecorded;
import com.memeboo2.haemi.guardian.report.domain.ReportParticipation;
import com.memeboo2.haemi.guardian.report.infrastructure.ReportParticipationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AttendanceRecordedListener {

    private final ReportParticipationRepository repository;

    /**
     * 활동 종류 플래그를 스냅샷에 멱등하게 미러링한다. 중복 수신에도 안전하다.
     * @ApplicationModuleListener가 트랜잭션을 열어 managed 엔티티가 변경 감지로 저장된다.
     */
    @ApplicationModuleListener
    public void on(AttendanceRecorded event) {
        ReportParticipation participation = repository
                .findByElderIdAndParticipationDate(event.elderId(), event.participationDate())
                .orElse(null);

        if (participation == null) {
            ReportParticipation created = ReportParticipation.of(event.elderId(), event.participationDate());
            created.mark(event.activityType());
            repository.saveAndFlush(created);
        } else {
            participation.mark(event.activityType());
        }
    }
}
