package com.sleepwell.sleepwell_backend.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 활동 이벤트 타입
 *
 * 사용자의 다양한 활동을 추적하기 위한 이벤트 타입을 정의합니다.
 * 로그인, AI 기능 사용, 화면 조회 등의 이벤트를 체계적으로 분류합니다.
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum ActivityEventType {

    // ===== 인증 관련 =====
    /**
     * 로그인 이벤트
     * 사용자가 성공적으로 로그인했을 때 발생
     */
    LOGIN("로그인", "authentication"),

    /**
     * 로그아웃 이벤트
     * 사용자가 로그아웃했을 때 발생
     */
    LOGOUT("로그아웃", "authentication"),

    // ===== AI 기능 관련 =====
    /**
     * AI 졸림 상담 기능 사용
     * 사용자가 졸림 관련 AI 상담을 시작했을 때 발생
     */
    AI_SLEEPY("AI 졸림 상담", "ai_feature"),

    /**
     * AI 불면 상담 기능 사용
     * 사용자가 불면 관련 AI 상담을 시작했을 때 발생
     */
    AI_INSOMNIA("AI 불면 상담", "ai_feature"),

    /**
     * AI 수면검사 기능 사용
     * 사용자가 AI 수면검사를 시작했을 때 발생
     */
    AI_SLEEP_TEST("AI 수면검사", "ai_feature"),

    /**
     * AI 스트레스 상담 기능 사용
     * 사용자가 스트레스 관련 AI 상담을 시작했을 때 발생
     */
    AI_STRESS("AI 스트레스 상담", "ai_feature"),

    /**
     * AI 경혈 상담 기능 사용
     * 사용자가 경혈 관련 AI 상담을 시작했을 때 발생
     */
    AI_ACUPRESSURE("AI 경혈 상담", "ai_feature"),

    /**
     * AI 일반 상담 기능 사용
     * RAG 기반 범용 AI 상담을 시작했을 때 발생 (카테고리 미분류)
     */
    AI_GENERAL("AI 일반 상담", "ai_feature"),

    // ===== 확장 가능한 이벤트 타입 =====
    /**
     * 화면 조회 이벤트
     * 특정 화면을 조회했을 때 발생 (향후 확장용)
     */
    SCREEN_VIEW("화면 조회", "navigation"),

    /**
     * 기능 사용 이벤트
     * 일반적인 기능 사용 시 발생 (향후 확장용)
     */
    FEATURE_USE("기능 사용", "feature");

    /**
     * 이벤트 타입의 한글 표시명
     */
    private final String displayName;

    /**
     * 이벤트 카테고리 (분류 목적)
     * - authentication: 인증 관련
     * - ai_feature: AI 기능 관련
     * - navigation: 화면 이동 관련
     * - feature: 일반 기능 관련
     */
    private final String category;

    /**
     * 이벤트 타입이 AI 기능 관련인지 확인
     *
     * @return AI 기능 이벤트인 경우 true
     */
    public boolean isAIFeature() {
        return this.category.equals("ai_feature");
    }

    /**
     * 이벤트 타입이 인증 관련인지 확인
     *
     * @return 인증 이벤트인 경우 true
     */
    public boolean isAuthentication() {
        return this.category.equals("authentication");
    }
}
