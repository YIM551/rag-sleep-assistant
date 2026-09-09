package com.sleepwell.sleepwell_backend.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 분석 작업 우선순위
 */
@Getter
@RequiredArgsConstructor
public enum JobPriority {
    HIGH(1, "높음"),
    NORMAL(3, "보통"),
    LOW(5, "낮음");
    
    private final int value;
    private final String description;
    
    /**
     * 우선순위 값으로 enum 찾기
     */
    public static JobPriority fromValue(int value) {
        for (JobPriority priority : values()) {
            if (priority.value == value) {
                return priority;
            }
        }
        return NORMAL; // 기본값
    }
}