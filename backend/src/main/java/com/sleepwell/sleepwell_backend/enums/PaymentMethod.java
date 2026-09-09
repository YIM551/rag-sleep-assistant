package com.sleepwell.sleepwell_backend.enums;

/**
 * 결제 방법 열거형
 */
public enum PaymentMethod {
    CARD,               // 카드 (신용/체크)
    CREDIT_CARD,        // 신용카드
    BANK_TRANSFER,      // 계좌이체
    MOBILE,             // 휴대폰 결제
    KAKAOPAY,           // 카카오페이
    NAVERPAY,           // 네이버페이
    TOSSPAY             // 토스페이
} 