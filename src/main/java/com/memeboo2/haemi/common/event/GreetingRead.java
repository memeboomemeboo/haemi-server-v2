package com.memeboo2.haemi.common.event;

import org.springframework.modulith.events.Externalized;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 어르신이 하루 한마디를 처음 읽었을 때 발행한다.
 * 최초 열람 1회만 발행한다(재열람 시 미발행). elder/attendance가 그날의 참여 활동으로 기록한다.
 * readDate는 KST 기준. 수신 측이 clock.today()로 유추하면 재전달·지연 시 날짜가 틀어지므로
 * 발생 날짜를 이벤트에 싣는다 (TrainingSessionCompleted와 동일한 형태).
 */
@Externalized
public record GreetingRead(UUID elderId, UUID dailyCareId, LocalDate readDate) {}
