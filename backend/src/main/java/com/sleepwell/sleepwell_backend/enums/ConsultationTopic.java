package com.sleepwell.sleepwell_backend.enums;

/**
 * 상담 주제
 * 음성 상담에서 다루는 주요 수면 관련 주제들을 나타냅니다.
 */
public enum ConsultationTopic {
    INSOMNIA("불면증"),
    SLEEP_APNEA("수면무호흡증"),
    SNORING("코골이"),
    SLEEP_SCHEDULE("수면 스케줄"),
    SLEEP_QUALITY("수면 품질"),
    SLEEP_HYGIENE("수면 위생"),
    STRESS_SLEEP("스트레스와 수면"),
    MEDICATION_SLEEP("약물과 수면"),
    SLEEP_DISORDERS("수면 장애"),
    GENERAL("일반 상담");

    private final String description;

    ConsultationTopic(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayName() {
        return description;
    }
} 