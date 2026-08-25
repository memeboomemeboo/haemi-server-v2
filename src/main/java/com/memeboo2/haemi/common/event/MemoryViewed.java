package com.memeboo2.haemi.common.event;

import org.springframework.modulith.events.Externalized;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 어르신이 추억을 처음 열어봤을 때 발행한다.
 * 최초 열람 1회만 발행한다(재열람 시 미발행). elder/attendance가 그날의 참여 활동으로 기록한다.
 * viewedDate는 KST 기준. TrainingSessionCompleted와 동일한 형태로 발생 날짜를 싣는다.
 */
@Externalized
public record MemoryViewed(UUID elderId, UUID memoryId, LocalDate viewedDate) {}
