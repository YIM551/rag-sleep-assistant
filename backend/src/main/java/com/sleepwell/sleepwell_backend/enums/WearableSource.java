package com.sleepwell.sleepwell_backend.enums;

/**
 * 웨어러블 기기 소스 열거형
 * Context7 베스트 프랙티스: 명확한 명명과 문서화
 */
public enum WearableSource {
    SAMSUNG_HEALTH("삼성 헬스", "Samsung Health"),     
    APPLE_HEALTH("애플 헬스", "Apple Health"),       
    HEALTH_CONNECT("헬스 커넥트", "Health Connect"),   // Android Health Connect
    FLUTTER_HEALTH("플러터 헬스", "Flutter Health"),   // Flutter Health plugin
    FITBIT("핏빗", "Fitbit"),             
    GARMIN("가민", "Garmin"),             
    MANUAL("수동 입력", "Manual Input");              

    private final String koreanName;
    private final String englishName;

    WearableSource(String koreanName, String englishName) {
        this.koreanName = koreanName;
        this.englishName = englishName;
    }

    public String getKoreanName() {
        return koreanName;
    }

    public String getEnglishName() {
        return englishName;
    }

    /**
     * 플랫폼이 모바일 플랫폼인지 확인
     */
    public boolean isMobilePlatform() {
        return this == SAMSUNG_HEALTH || this == APPLE_HEALTH || this == HEALTH_CONNECT || this == FLUTTER_HEALTH;
    }

    /**
     * 플랫폼이 웨어러블 기기인지 확인
     */
    public boolean isWearableDevice() {
        return this == FITBIT || this == GARMIN;
    }
} 