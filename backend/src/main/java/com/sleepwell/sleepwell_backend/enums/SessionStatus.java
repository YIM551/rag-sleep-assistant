package com.sleepwell.sleepwell_backend.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 상담 세션 상태
 */
@Getter
@RequiredArgsConstructor
public enum SessionStatus {
    SCHEDULED("예약됨"),
    IN_PROGRESS("진행중"),
    COMPLETED("완료"),
    CANCELLED("취소됨"),
    FAILED("실패");
    
    private final String description;
}