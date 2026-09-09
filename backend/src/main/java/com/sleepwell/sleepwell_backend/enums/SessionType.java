package com.sleepwell.sleepwell_backend.enums;

/**
 * 세션 유형
 * 음성 상담 세션의 목적과 유형을 나타냅니다.
 */
public enum SessionType {
    SLEEP_ANALYSIS("수면 분석 상담"),
    SLEEP_IMPROVEMENT("수면 개선 상담"),
    GENERAL_CONSULTATION("일반 상담"),
    EMERGENCY("응급 상담"),
    AI_TRIGGERED("AI 자동 트리거 상담"),
    AI_CONSULTATION("AI 상담"),
    RAG_CONSULTATION("RAG 논문 기반 상담"),
    AUTOMATIC_ANALYSIS("자동 분석");

    private final String description;

    SessionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 