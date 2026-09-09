package com.sleepwell.sleepwell_backend.enums;

/**
 * 처리 유형
 * 음성 처리 작업의 종류를 나타냅니다.
 */
public enum ProcessingType {
    STT("음성→텍스트"),
    TTS("텍스트→음성"),
    SENTIMENT_ANALYSIS("감정 분석"),
    INTENT_RECOGNITION("의도 인식"),
    VOICE_ENHANCEMENT("음성 품질 향상"),
    NOISE_REDUCTION("노이즈 제거");

    private final String description;

    ProcessingType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 