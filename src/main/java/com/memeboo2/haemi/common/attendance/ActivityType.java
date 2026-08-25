package com.memeboo2.haemi.common.attendance;

/**
 * 어르신이 그날 완료한 참여 활동의 종류. 보호자 홈 요일별 스택 막대의 색 세그먼트.
 * 횟수가 아니라 "무엇을 했다"만 나타낸다 (D13: 인지 활동 N회 미사용).
 */
public enum ActivityType {
    TRAINING,
    GREETING_READ,
    MEMORY_VIEWED,
    REPLIED
}
