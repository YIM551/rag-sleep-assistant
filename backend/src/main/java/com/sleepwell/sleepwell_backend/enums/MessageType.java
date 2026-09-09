package com.sleepwell.sleepwell_backend.enums;

/**
 * 메시지 타입
 * 음성 상담 중 교환되는 메시지의 유형을 나타냅니다.
 */
public enum MessageType {
    USER("사용자"),
    AI("AI"),
    USER_VOICE("사용자 음성"),
    USER_TEXT("사용자 텍스트"),
    AI_RESPONSE("AI 응답"),
    SYSTEM_MESSAGE("시스템 메시지");

    private final String description;

    MessageType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 