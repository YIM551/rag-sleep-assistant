package com.sleepwell.sleepwell_backend.enums;

/**
 * 수면 오디오 이벤트 유형 열거형 (AudioEventType Enum)
 * 
 * 웨어러블 기기에서 감지 가능한 수면 중 오디오 이벤트 유형을 정의합니다.
 * Flutter Health SDK 및 워치 자체 AI 분석 결과를 기반으로 분류됩니다.
 * 
 * 각 이벤트 유형별 특징:
 * - SNORING: 코골이 - 호흡기 관련, 수면무호흡 위험 지표
 * - BRUXISM: 이갈이 - 치아/턱 관련, 스트레스 지표  
 * - SLEEP_TALKING: 잠꼬대 - 수면 단계 분석, 정신적 스트레스 지표
 * - ENVIRONMENTAL_NOISE: 환경 소음 - 수면 방해 요소 분석
 * 
 * @author SleepWell Development Team
 * @since 1.0
 * @see SleepAudioEvent
 */
public enum AudioEventType {
    
    /**
     * 코골이 (Snoring)
     * 
     * 특징:
     * - 호흡기 진동으로 인한 소음
     * - 수면무호흡증과 높은 상관관계
     * - 강도에 따라 의료진 상담 필요
     * - 수면 품질 저하의 주요 원인
     */
    SNORING("코골이", "호흡기 진동으로 인한 소음", true),
    
    /**
     * 이갈이 (Bruxism/Teeth Grinding)
     * 
     * 특징:
     * - 치아 마찰로 인한 소음
     * - 스트레스, 불안과 높은 상관관계
     * - 치아 손상 및 턱관절 장애 위험
     * - 주로 REM 수면 단계에서 발생
     */
    BRUXISM("이갈이", "치아 마찰로 인한 소음", true),
    
    /**
     * 잠꼬대 (Sleep Talking)
     * 
     * 특징:
     * - 수면 중 발성
     * - 정신적 스트레스, 꿈과 연관
     * - 일반적으로 무해하나 빈도 증가 시 주의
     * - 수면 단계 분석에 유용한 지표
     */
    SLEEP_TALKING("잠꼬대", "수면 중 발성", false),
    
    /**
     * 환경 소음 (Environmental Noise)
     * 
     * 특징:
     * - 외부 환경으로 인한 소음
     * - 수면 방해 요소 분석
     * - 수면 환경 개선 지표
     * - dB 수준 측정 중심
     */
    ENVIRONMENTAL_NOISE("환경 소음", "외부 환경으로 인한 소음", false);

    private final String displayName;
    private final String description;
    private final boolean requiresMonitoring;

    /**
     * AudioEventType 생성자
     * 
     * @param displayName 화면 표시용 이름
     * @param description 이벤트 설명
     * @param requiresMonitoring 지속적 모니터링 필요 여부
     */
    AudioEventType(String displayName, String description, boolean requiresMonitoring) {
        this.displayName = displayName;
        this.description = description;
        this.requiresMonitoring = requiresMonitoring;
    }

    /**
     * 화면 표시용 이름 반환
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 이벤트 설명 반환
     */
    public String getDescription() {
        return description;
    }

    /**
     * 지속적 모니터링 필요 여부
     */
    public boolean isRequiresMonitoring() {
        return requiresMonitoring;
    }

    /**
     * 의료적 관심이 필요한 이벤트 여부
     */
    public boolean isMedicallyRelevant() {
        return this == SNORING || this == BRUXISM;
    }

    /**
     * 수면 품질에 직접적 영향을 주는 이벤트 여부
     */
    public boolean affectsSleepQuality() {
        return this == SNORING || this == ENVIRONMENTAL_NOISE;
    }

    /**
     * 스트레스 관련 이벤트 여부
     */
    public boolean isStressRelated() {
        return this == BRUXISM || this == SLEEP_TALKING;
    }

    /**
     * 문자열로부터 AudioEventType 찾기 (대소문자 무시)
     * 
     * @param value 검색할 문자열
     * @return 매칭되는 AudioEventType, 없으면 null
     */
    public static AudioEventType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        String normalizedValue = value.trim().toUpperCase();
        
        try {
            return AudioEventType.valueOf(normalizedValue);
        } catch (IllegalArgumentException e) {
            // displayName으로도 검색 시도
            for (AudioEventType type : AudioEventType.values()) {
                if (type.getDisplayName().equals(value.trim())) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * 권장 강도 임계값 반환
     * 의료진 상담이 필요한 강도 수준
     */
    public int getRecommendedThreshold() {
        switch (this) {
            case SNORING:
                return 8; // 강도 8 이상 시 수면무호흡 의심
            case BRUXISM:
                return 7; // 강도 7 이상 시 치과 상담 권장
            case SLEEP_TALKING:
                return 9; // 매우 빈번한 경우만 주의
            case ENVIRONMENTAL_NOISE:
                return 6; // 수면 방해 수준
            default:
                return 10;
        }
    }
} 