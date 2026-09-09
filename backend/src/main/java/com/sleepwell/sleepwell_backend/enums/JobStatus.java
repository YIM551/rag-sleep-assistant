package com.sleepwell.sleepwell_backend.enums;

/**
 * 작업 상태
 * 음성 처리 작업의 현재 진행 상태를 나타냅니다.
 */
public enum JobStatus {
    QUEUED("대기 중"),
    PROCESSING("처리 중"),
    COMPLETED("완료됨"),
    FAILED("실패함"),
    CANCELLED("취소됨");

    private final String description;

    JobStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 