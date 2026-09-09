package com.sleepwell.sleepwell_backend.enums;

/**
 * 사용자 권한 열거형
 * Spring Security 권한 체계에 사용됩니다.
 */
public enum UserRole {
    /**
     * 일반 사용자
     */
    USER("ROLE_USER"),
    
    /**
     * 관리자
     */
    ADMIN("ROLE_ADMIN"),
    
    /**
     * 프리미엄 사용자 (구독자)
     */
    PREMIUM("ROLE_PREMIUM");
    
    private final String authority;
    
    UserRole(String authority) {
        this.authority = authority;
    }
    
    public String getAuthority() {
        return authority;
    }
} 