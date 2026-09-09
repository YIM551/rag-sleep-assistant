package com.sleepwell.sleepwell_backend.enums;

/**
 * ASMR 콘텐츠 상태
 * ASMR 콘텐츠의 공개 및 관리 상태를 나타냅니다.
 */
public enum ASMRContentStatus {
    DRAFT("임시저장"),
    ACTIVE("활성"),
    INACTIVE("비활성"),
    ARCHIVED("보관");

    private final String description;

    ASMRContentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayName() {
        return description;
    }
}