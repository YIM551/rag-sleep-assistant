package com.sleepwell.sleepwell_backend.enums;

/**
 * ASMR 콘텐츠 카테고리
 * ASMR 콘텐츠의 유형을 분류합니다.
 */
public enum ASMRCategory {
    NATURE("자연음"),
    WHITE_NOISE("백색소음"),
    BINAURAL("바이노럴 비트"),
    AMBIENT("환경음"),
    GUIDED("가이드 음성"),
    CLASSICAL("클래식");

    private final String description;

    ASMRCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayName() {
        return description;
    }
}