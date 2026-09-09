package com.sleepwell.sleepwell_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 수면 피드백 엔티티 (SleepFeedback Entity)
 * 
 * 사용자의 주관적인 수면 경험과 만족도를 수집하여 머신러닝 모델의
 * 정확도 향상과 개인화된 분석을 위한 라벨 데이터로 활용하는 엔티티입니다.
 * 
 * 수집 피드백 범위:
 * - 수면 만족도: 전반적 만족도, 피로 회복도
 * - 수면 경험: 잠들기까지의 시간 체감, 중간 각성 인지
 * - 기상 후 상태: 상쾌함, 컨디션, 집중력
 * - 수면 환경 평가: 온도, 소음, 조명 적절성
 * - 개선 요청: 구체적인 불편 사항과 개선 희망사항
 * 
 * ML 활용 목적:
 * - Phase 2: 객관적 데이터와 주관적 만족도 간 상관관계 분석
 * - Phase 3: 피드백 기반 개인화 권장사항 검증 및 개선
 * - Phase 4: 감정과 만족도 예측 모델 학습
 * - Validation: AI 분석 결과와 사용자 체감 간 일치도 검증
 * 
 * 성능 최적화:
 * - 사용자별 최신 피드백 조회 최적화
 * - 만족도 기반 트렌드 분석 최적화
 * - 수면 기록과의 조인 최적화
 * 
 * @author SleepWell Development Team
 * @since 1.0
 * @see User
 * @see SleepRecord
 * @see BaseEntity
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(indexes = {
    // 사용자별 최신 피드백 조회 (대시보드 피드백 트렌드)
    @Index(name = "IDX_SLEEP_FEEDBACK_USER_DATE", columnList = "user_id, feedbackDate"),
    // 수면 기록과의 연결 조회 (객관적 데이터와 주관적 피드백 매칭)
    @Index(name = "IDX_SLEEP_FEEDBACK_RECORD", columnList = "sleep_record_id"),
    // 만족도 기반 분석 (높은/낮은 만족도 패턴 분석)
    @Index(name = "IDX_SLEEP_FEEDBACK_SATISFACTION", columnList = "overallSatisfaction, feedbackDate"),
    // 기상 후 컨디션 분석 (컨디션 평가 트렌드)
    @Index(name = "IDX_SLEEP_FEEDBACK_CONDITION", columnList = "morningCondition, feedbackDate"),
    // 피드백 수집 시간 분석 (즉시 vs 지연 피드백 품질 분석)
    @Index(name = "IDX_SLEEP_FEEDBACK_TIMING", columnList = "feedbackCollectedAt, feedbackDate"),
    // 피로 회복도 분석 (수면 효과성 주관적 평가)
    @Index(name = "IDX_SLEEP_FEEDBACK_RECOVERY", columnList = "fatigueRecovery, overallSatisfaction")
})
public class SleepFeedback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 피드백 제공 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "FK_SLEEP_FEEDBACK_USER"))
    private User user;

    /**
     * 연관된 수면 기록
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sleep_record_id", nullable = false,
                foreignKey = @ForeignKey(name = "FK_SLEEP_FEEDBACK_SLEEP_RECORD"))
    private SleepRecord sleepRecord;

    /**
     * 피드백 대상 일자
     */
    @Column(nullable = false)
    private LocalDate feedbackDate;

    // === 전반적 수면 만족도 ===

    /**
     * 전체 수면 만족도 (1-5 척도)
     * 1: 매우 불만족, 2: 불만족, 3: 보통, 4: 만족, 5: 매우 만족
     */
    @Column(nullable = false)
    private Integer overallSatisfaction;

    /**
     * 피로 회복 정도 (1-5 척도)
     * 1: 전혀 회복되지 않음, 5: 완전히 회복됨
     */
    @Column(nullable = false)
    private Integer fatigueRecovery;

    /**
     * 수면 깊이 체감 (1-5 척도)
     * 1: 매우 얕은 잠, 5: 매우 깊은 잠
     */
    private Integer perceivedSleepDepth;

    // === 수면 과정 평가 ===

    /**
     * 잠들기까지 걸린 시간 체감 (분)
     * 사용자가 생각하는 실제 잠들기까지의 시간
     */
    private Integer perceivedSleepLatency;

    /**
     * 잠들기 난이도 (1-5 척도)
     * 1: 매우 쉬움, 5: 매우 어려움
     */
    private Integer sleepOnsetDifficulty;

    /**
     * 중간 각성 인지 횟수
     * 사용자가 기억하는 밤중에 깬 횟수
     */
    @Builder.Default
    private Integer perceivedWakeupCount = 0;

    /**
     * 각성 후 재입면 난이도 (1-5 척도)
     * 1: 매우 쉬움, 5: 매우 어려움
     */
    private Integer backToSleepDifficulty;

    // === 기상 후 상태 ===

    /**
     * 기상 시 상쾌함 (1-5 척도)
     * 1: 전혀 상쾌하지 않음, 5: 매우 상쾌함
     */
    @Column(nullable = false)
    private Integer morningFreshness;

    /**
     * 오전 컨디션 (1-5 척도)
     * 1: 매우 나쁨, 5: 매우 좋음
     */
    @Column(nullable = false)
    private Integer morningCondition;

    /**
     * 낮 시간 졸림 정도 (1-5 척도)
     * 1: 전혀 졸리지 않음, 5: 매우 졸림
     */
    private Integer daytimeSleepiness;

    /**
     * 집중력 수준 (1-5 척도)
     * 1: 매우 낮음, 5: 매우 높음
     */
    private Integer concentrationLevel;

    /**
     * 기분 상태 (1-5 척도)
     * 1: 매우 나쁨, 5: 매우 좋음
     */
    private Integer moodRating;

    // === 수면 환경 평가 ===

    /**
     * 침실 온도 적절성 (1-5 척도)
     * 1: 매우 부적절, 5: 매우 적절
     */
    private Integer temperatureComfort;

    /**
     * 소음 수준 만족도 (1-5 척도)
     * 1: 매우 시끄러움, 5: 매우 조용함
     */
    private Integer noiseComfort;

    /**
     * 조명 적절성 (1-5 척도)
     * 1: 매우 부적절, 5: 매우 적절
     */
    private Integer lightingComfort;

    /**
     * 침구 편안함 (1-5 척도)
     * 1: 매우 불편함, 5: 매우 편안함
     */
    private Integer beddingComfort;

    // === 수면 방해 요소 ===

    /**
     * 코골이 인지 여부
     */
    @Builder.Default
    private Boolean perceivedSnoring = false;

    /**
     * 파트너/가족 방해 여부
     */
    @Builder.Default
    private Boolean partnerDisturbance = false;

    /**
     * 외부 소음 방해 여부
     */
    @Builder.Default
    private Boolean externalNoiseDisturbance = false;

    /**
     * 스트레스/걱정으로 인한 방해 여부
     */
    @Builder.Default
    private Boolean stressDisturbance = false;

    /**
     * 신체적 불편함 여부 (목, 어깨, 허리 등)
     */
    @Builder.Default
    private Boolean physicalDiscomfort = false;

    // === 자유 텍스트 피드백 ===

    /**
     * 수면 중 특이사항
     */
    @Column(columnDefinition = "TEXT")
    private String sleepNotes;

    /**
     * 꿈의 기억 여부와 내용
     */
    @Column(columnDefinition = "TEXT")
    private String dreamRecall;

    /**
     * 개선이 필요한 부분
     */
    @Column(columnDefinition = "TEXT")
    private String improvementSuggestions;

    /**
     * 어제와 다른 점
     */
    @Column(columnDefinition = "TEXT")
    private String comparisonWithPrevious;

    // === 메타데이터 ===

    /**
     * 피드백 수집 시간
     */
    @Builder.Default
    private LocalDateTime feedbackCollectedAt = LocalDateTime.now();

    /**
     * 피드백 수집 방법 (MORNING_SURVEY, EVENING_SURVEY, REAL_TIME, NOTIFICATION_PROMPT)
     */
    @Column(length = 30)
    @Builder.Default
    private String collectionMethod = "MORNING_SURVEY";

    /**
     * 피드백 완성도 (1-100%)
     * 사용자가 얼마나 많은 항목을 입력했는지
     */
    private Integer completionPercentage;

    /**
     * 신뢰도 점수 (1-5)
     * 피드백 수집 시간, 일관성 등을 고려한 신뢰도
     */
    private Integer reliabilityScore;

    // === 비즈니스 메서드 ===

    /**
     * 전반적으로 좋은 수면인지 판단
     */
    public boolean isGoodSleep() {
        return overallSatisfaction >= 4 && fatigueRecovery >= 4 && morningFreshness >= 4;
    }

    /**
     * 수면 문제가 있는지 판단
     */
    public boolean hasSleepProblems() {
        return overallSatisfaction <= 2 || 
               fatigueRecovery <= 2 || 
               sleepOnsetDifficulty >= 4 ||
               daytimeSleepiness >= 4;
    }

    /**
     * 환경적 문제가 있는지 판단
     */
    public boolean hasEnvironmentalIssues() {
        return (temperatureComfort != null && temperatureComfort <= 2) ||
               (noiseComfort != null && noiseComfort <= 2) ||
               (lightingComfort != null && lightingComfort <= 2);
    }

    /**
     * 즉시 수집된 피드백인지 확인 (수면 후 4시간 이내)
     */
    public boolean isImmediateFeedback() {
        if (feedbackCollectedAt == null || sleepRecord == null || sleepRecord.getSleepEndTime() == null) {
            return false;
        }
        return feedbackCollectedAt.isBefore(sleepRecord.getSleepEndTime().plusHours(4));
    }

    /**
     * 피드백 품질 점수 계산 (1-100)
     */
    public int calculateFeedbackQuality() {
        int qualityScore = 0;
        int totalFactors = 0;

        // 즉시성 (30점)
        if (isImmediateFeedback()) {
            qualityScore += 30;
        } else if (feedbackCollectedAt.isBefore(sleepRecord.getSleepEndTime().plusHours(12))) {
            qualityScore += 20;
        } else {
            qualityScore += 10;
        }
        totalFactors += 30;

        // 완성도 (40점)
        if (completionPercentage != null) {
            qualityScore += (completionPercentage * 40) / 100;
        } else {
            qualityScore += 20; // 기본값
        }
        totalFactors += 40;

        // 일관성 (30점)
        if (reliabilityScore != null) {
            qualityScore += (reliabilityScore * 30) / 5;
        } else {
            qualityScore += 15; // 기본값
        }
        totalFactors += 30;

        return qualityScore;
    }
} 