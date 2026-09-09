package com.sleepwell.sleepwell_backend.enums;

/**
 * 결제 상태 열거형
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
public enum PaymentStatus {
    /**
     * 결제 대기 중
     */
    PENDING("결제 대기"),
    
    /**
     * 결제 진행 중
     */
    PROCESSING("결제 진행 중"),
    
    /**
     * 결제 완료
     */
    COMPLETED("결제 완료"),
    
    /**
     * 결제 실패
     */
    FAILED("결제 실패"),
    
    /**
     * 결제 취소
     */
    CANCELLED("결제 취소"),
    
    /**
     * 부분 환불
     */
    PARTIALLY_REFUNDED("부분 환불"),
    
    /**
     * 전액 환불
     */
    FULLY_REFUNDED("전액 환불"),
    
    /**
     * 환불 (일반)
     */
    REFUNDED("환불");
    
    private final String description;
    
    PaymentStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}