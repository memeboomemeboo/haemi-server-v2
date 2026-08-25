package com.memeboo2.haemi.guardian.report.listener;

import com.memeboo2.haemi.common.event.AttendanceRecorded;
import com.memeboo2.haemi.common.persistence.UuidGenerator;
import com.memeboo2.haemi.guardian.report.infrastructure.ReportParticipationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AttendanceRecordedListener {

    private final ReportParticipationRepository repository;

    /**
     * 표준 MERGE로 원자적으로 적재한다. exists 검사 후 saveAndFlush를 하면
     * 중복 수신 시 unique 위반이 REQUIRES_NEW 트랜잭션을 커밋 불가 상태로 만들어,
     * catch로 잡아도 실제로는 실패·재시도로 남는다.
     */
    @ApplicationModuleListener
    public void on(AttendanceRecorded event) {
        repository.insertIfAbsent(UuidGenerator.generate(), event.elderId(), event.participationDate());
    }
}
