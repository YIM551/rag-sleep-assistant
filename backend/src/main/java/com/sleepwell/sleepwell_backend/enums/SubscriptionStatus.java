package com.sleepwell.sleepwell_backend.enums;

/**
 * 구독 상태 열거형
 */
public enum SubscriptionStatus {
    ACTIVE,         // 활성
    EXPIRED,        // 만료
    CANCELLED,      // 취소
    SUSPENDED,      // 일시정지
    PENDING         // 대기 (결제 대기 등)
} 