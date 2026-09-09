package com.sleepwell.sleepwell_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자 활동 통계 엔티티
 *
 * 사용자별 활동 집계 정보를 저장합니다.
 * UserActivityEvent 테이블의 상세 이벤트를 기반으로 카운트를 집계하여
 * 빠른 조회가 가능하도록 최적화된 통계 데이터를 제공합니다.
 *
 * 주요 용도:
 * - 사용자별 로그인 빈도 조회
 * - AI 기능별 사용 통계 (5가지 타입)
 * - 관리자 대시보드 통계 표시
 * - PM/기획팀 리포팅 데이터 제공
 *
 * 성능 최적화:
 * - userId를 PK로 사용하여 조회 최적화
 * - 이벤트 발생 시마다 집계 업데이트 (REQUIRES_NEW 트랜잭션)
 * - 별도 배치 작업으로 정합성 보정 가능
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Entity
@Table(name = "user_activity_statistics")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityStatistics {

    /**
     * 사용자 ID (Primary Key)
     * User 테이블의 PK와 동일
     */
    @Id
    @Column(name = "user_id")
    private Long userId;

    /**
     * 사용자 참조
     * 연관 관계 편의를 위한 참조
     */
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // userId를 User의 PK와 공유
    @JoinColumn(name = "user_id")
    private User user;

    // ===== 로그인 통계 =====

    /**
     * 총 로그인 횟수
     * 사용자가 로그인한 총 누적 횟수
     */
    @Column(name = "login_count", nullable = false)
    @Builder.Default
    private Integer loginCount = 0;

    /**
     * 최근 로그인 시각
     * 마지막으로 로그인한 시각
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // ===== AI 기능 사용 통계 =====

    /**
     * AI 졸림 상담 사용 횟수
     * AI_SLEEPY 이벤트 발생 누적 횟수
     */
    @Column(name = "ai_sleepy_count", nullable = false)
    @Builder.Default
    private Integer aiSleepyCount = 0;

    /**
     * AI 불면 상담 사용 횟수
     * AI_INSOMNIA 이벤트 발생 누적 횟수
     */
    @Column(name = "ai_insomnia_count", nullable = false)
    @Builder.Default
    private Integer aiInsomniaCount = 0;

    /**
     * AI 수면검사 사용 횟수
     * AI_SLEEP_TEST 이벤트 발생 누적 횟수
     */
    @Column(name = "ai_sleep_test_count", nullable = false)
    @Builder.Default
    private Integer aiSleepTestCount = 0;

    /**
     * AI 스트레스 상담 사용 횟수
     * AI_STRESS 이벤트 발생 누적 횟수
     */
    @Column(name = "ai_stress_count", nullable = false)
    @Builder.Default
    private Integer aiStressCount = 0;

    /**
     * AI 경혈 상담 사용 횟수
     * AI_ACUPRESSURE 이벤트 발생 누적 횟수
     */
    @Column(name = "ai_acupressure_count", nullable = false)
    @Builder.Default
    private Integer aiAcupressureCount = 0;

    /**
     * AI 일반 상담 사용 횟수
     * AI_GENERAL 이벤트 발생 누적 횟수 (RAG 상담 등 카테고리 미분류)
     */
    @Column(name = "ai_general_count", nullable = false)
    @Builder.Default
    private Integer aiGeneralCount = 0;

    /**
     * 최근 AI 기능 사용 시각
     * 가장 최근에 AI 기능을 사용한 시각
     */
    @Column(name = "last_ai_feature_used_at")
    private LocalDateTime lastAiFeatureUsedAt;

    // ===== 메타 정보 =====

    /**
     * 통계 생성 시각
     */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 통계 최종 업데이트 시각
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ===== 비즈니스 메서드 =====

    /**
     * 로그인 카운트 증가
     * 로그인 이벤트 발생 시 호출
     */
    public void incrementLogin() {
        this.loginCount++;
        this.lastLoginAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * AI 기능 카운트 증가
     * AI 기능 사용 이벤트 발생 시 카테고리에 따라 호출
     *
     * @param category AI 기능 카테고리 (SLEEPY, INSOMNIA, SLEEP_TEST, STRESS, ACUPRESSURE, GENERAL, RAG_CONSULTATION)
     */
    public void incrementAIClick(String category) {
        if (category == null) {
            return;
        }

        switch (category.toUpperCase()) {
            case "SLEEPY":
                this.aiSleepyCount++;
                break;
            case "INSOMNIA":
                this.aiInsomniaCount++;
                break;
            case "SLEEP_TEST":
                this.aiSleepTestCount++;
                break;
            case "STRESS":
                this.aiStressCount++;
                break;
            case "ACUPRESSURE":
                this.aiAcupressureCount++;
                break;
            case "GENERAL":
            case "RAG_CONSULTATION":
                this.aiGeneralCount++;
                break;
            default:
                // 알 수 없는 카테고리는 무시
                return;
        }

        this.lastAiFeatureUsedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 총 AI 기능 사용 횟수 조회
     *
     * @return 모든 AI 기능 사용 횟수의 합
     */
    public int getTotalAIFeatureCount() {
        return aiSleepyCount + aiInsomniaCount + aiSleepTestCount +
               aiStressCount + aiAcupressureCount + aiGeneralCount;
    }

    /**
     * 가장 많이 사용한 AI 기능 조회
     *
     * @return 가장 많이 사용한 AI 기능 카테고리명 (횟수가 같으면 첫 번째 반환)
     */
    public String getMostUsedAIFeature() {
        int maxCount = 0;
        String mostUsed = "NONE";

        if (aiSleepyCount > maxCount) {
            maxCount = aiSleepyCount;
            mostUsed = "SLEEPY";
        }
        if (aiInsomniaCount > maxCount) {
            maxCount = aiInsomniaCount;
            mostUsed = "INSOMNIA";
        }
        if (aiSleepTestCount > maxCount) {
            maxCount = aiSleepTestCount;
            mostUsed = "SLEEP_TEST";
        }
        if (aiStressCount > maxCount) {
            maxCount = aiStressCount;
            mostUsed = "STRESS";
        }
        if (aiAcupressureCount > maxCount) {
            mostUsed = "ACUPRESSURE";
        }

        return mostUsed;
    }

    /**
     * 마지막 로그인 시각 업데이트
     * 테스트용 메서드
     */
    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 마지막 AI 기능 사용 시각 업데이트
     * 테스트용 메서드
     */
    public void updateLastAIFeatureUsedAt() {
        this.lastAiFeatureUsedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 엔티티 생성 시 자동 호출
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 엔티티 업데이트 시 자동 호출
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
