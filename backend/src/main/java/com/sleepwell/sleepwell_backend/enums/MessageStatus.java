package com.sleepwell.sleepwell_backend.enums;

/**
 * 메시지 처리 상태
 * 음성 상담 메시지의 처리 단계를 나타냅니다.
 */
public enum MessageStatus {
    PENDING("대기 중"),
    PROCESSING("처리 중"),
    DELIVERED("전송됨"),
    COMPLETED("완료됨"),
    FAILED("실패함");

    private final String description;

    MessageStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 