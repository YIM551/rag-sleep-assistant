package com.sleepwell.sleepwell_backend.enums;

/**
 * 알림 타입을 정의하는 enum
 */
public enum NotificationType {
    // 수면 관련 알림
    SLEEP_REMINDER,             // 수면 기록 시간 알림
    WAKE_UP_REMINDER,           // 기상 시간 알림
    SLEEP_RECORD_REMINDER,      // 수면 기록 작성 알림
    SLEEP_ANALYSIS_COMPLETE,    // 수면 분석 완료 알림
    PERSONALIZED_SLEEP_TIP,     // 개인화된 수면 팁 제공
    HEALTH_INSIGHT,            // 건강 통찰력 알림 (수면 패턴, 건강 상태 등)

    // 구독 및 결제 관련 알림
    SUBSCRIPTION_EXPIRING_SOON, // 구독 만료 임박 알림
    SUBSCRIPTION_EXPIRED,       // 구독 만료 알림
    PAYMENT_SUCCESS,           // 결제 성공 알림
    PAYMENT_FAILED,            // 결제 실패 알림

    // 일반 정보 및 공지
    NEW_FEATURE_UPDATE,        // 새로운 기능 업데이트 공지
    GENERAL_ANNOUNCEMENT,      // 일반 공지사항
    SYSTEM_MAINTENANCE,        // 시스템 점검 공지

    // 분석 관련 알림
    ANALYSIS_READY            // 분석 완료
}
