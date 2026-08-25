package com.memeboo2.haemi.common.event;

import org.springframework.modulith.events.Externalized;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 어르신이 추억에 응답(감정·댓글·이미지·음성)했을 때 발행한다.
 * respondedDate는 응답이 발생한 KST 날짜다. 수신 측(elder/attendance)이 clock.today()로
 * 유추하면 이벤트 재전달·지연 처리 시 날짜가 틀어지므로, 발생 날짜를 이벤트에 싣는다.
 * TrainingSessionCompleted(elderId, sessionDate)와 동일한 형태.
 */
@Externalized
public record ElderResponded(UUID memoryId, UUID elderId, LocalDate respondedDate) {}
