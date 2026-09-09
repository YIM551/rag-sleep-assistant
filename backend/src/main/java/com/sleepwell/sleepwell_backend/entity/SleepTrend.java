package com.sleepwell.sleepwell_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 수면 트렌드 엔티티 (SleepTrend Entity)
 * 
 * 사용자별 장기 수면 패턴 트렌드와 개인 기준선 데이터를 저장하여
 * 머신러닝 모델의 시계열 분석과 개인화 기준점 설정에 활용하는 엔티티입니다.
 * 
 * 트렌드 데이터 범위:
 * - 개인 기준선: 7일, 30일, 90일 기준 평균값과 표준편차
 * - 시계열 패턴: 요일별, 계절별, 월별 패턴 분석 결과
 * - 변화 추이: 개선/악화 트렌드, 변화율 분석
 * - 주기성 분석: 규칙적/불규칙적 패턴, 주기성 강도
 * - 이상치 감지: 평소 패턴에서 벗어난 비정상적 수면 기록
 * 
 * ML 활용 목적:
 * - Phase 2: 개인별 정상 범위 정의 및 이상치 감지
 * - Phase 3: 시계열 예측 모델의 기준선 데이터
 * - Phase 4: 개인화된 목표 설정 및 예측 정확도 향상
 * - Analytics: 인구 집단별 수면 패턴 비교 분석
 * 
 * 성능 최적화:
 * - 사용자별 최신 트렌드 조회 최적화
 * - 기간별 트렌드 비교 분석 최적화
 * - 트렌드 유형별 분석 최적화
 * 
 * @author SleepWell Development Team
 * @since 1.0
 * @see User
 * @see SleepRecord
 * @see SleepAnalysis
 * @see BaseEntity
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(indexes = {
    // 사용자별 최신 트렌드 조회 (대시보드 트렌드 정보)
    @Index(name = "IDX_SLEEP_TREND_USER_DATE", columnList = "user_id, trendDate"),
    // 트렌드 유형별 조회 (주간/월간/계절별 트렌드)
    @Index(name = "IDX_SLEEP_TREND_TYPE_DATE", columnList = "trendType, trendDate"),
    // 기간별 트렌드 비교 (특정 기간의 트렌드 변화 분석)
    @Index(name = "IDX_SLEEP_TREND_PERIOD", columnList = "periodDays, calculatedAt"),
    // 트렌드 점수 기반 분석 (개선/악화 추세 분석)
    @Index(name = "IDX_SLEEP_TREND_SCORE", columnList = "trendScore, trendType"),
    // 변화율 기반 분석 (급격한 변화 감지)
    @Index(name = "IDX_SLEEP_TREND_CHANGE_RATE", columnList = "changeRate, trendDate"),
    // 신뢰도 기반 분석 (높은 신뢰도 트렌드 우선 조회)
    @Index(name = "IDX_SLEEP_TREND_CONFIDENCE", columnList = "confidenceLevel, trendDate")
})
public class SleepTrend extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 트렌드 데이터 소유자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "FK_SLEEP_TREND_USER"))
    private User user;

    /**
     * 트렌드 기준 일자
     */
    @Column(nullable = false)
    private LocalDate trendDate;

    /**
     * 트렌드 유형 (WEEKLY, MONTHLY, QUARTERLY, SEASONAL, YEARLY)
     */
    @Column(length = 20, nullable = false)
    private String trendType;

    /**
     * 분석 기간 (일수)
     */
    @Column(nullable = false)
    private Integer periodDays;

    // === 개인 기준선 데이터 ===

    /**
     * 평균 수면 시간 (분)
     */
    private BigDecimal averageSleepMinutes;

    /**
     * 수면 시간 표준편차
     */
    private BigDecimal sleepMinutesStdDev;

    /**
     * 평균 수면 효율성 (%)
     */
    private BigDecimal averageSleepEfficiency;

    /**
     * 수면 효율성 표준편차
     */
    private BigDecimal sleepEfficiencyStdDev;

    /**
     * 평균 취침 시간 (시간, 24시간 형식)
     */
    private BigDecimal averageBedtimeHour;

    /**
     * 취침 시간 표준편차
     */
    private BigDecimal bedtimeStdDev;

    /**
     * 평균 기상 시간 (시간, 24시간 형식)
     */
    private BigDecimal averageWakeupHour;

    /**
     * 기상 시간 표준편차
     */
    private BigDecimal wakeupStdDev;

    /**
     * 평균 수면 점수
     */
    private BigDecimal averageSleepScore;

    /**
     * 수면 점수 표준편차
     */
    private BigDecimal sleepScoreStdDev;

    // === 수면 단계별 기준선 ===

    /**
     * 평균 깊은 잠 비율 (%)
     */
    private BigDecimal averageDeepSleepRatio;

    /**
     * 평균 REM 수면 비율 (%)
     */
    private BigDecimal averageRemSleepRatio;

    /**
     * 평균 얕은 잠 비율 (%)
     */
    private BigDecimal averageLightSleepRatio;

    /**
     * 평균 중간 각성 횟수
     */
    private BigDecimal averageWakeupCount;

    // === 트렌드 분석 결과 ===

    /**
     * 전체 트렌드 점수 (-100 ~ +100)
     * 음수: 악화 추세, 양수: 개선 추세
     */
    private Integer trendScore;

    /**
     * 변화율 (%)
     * 이전 기간 대비 주요 지표의 변화율
     */
    private BigDecimal changeRate;

    /**
     * 변화 방향 (IMPROVING, STABLE, DECLINING)
     */
    @Column(length = 20)
    private String trendDirection;

    /**
     * 규칙성 점수 (0-100)
     * 수면 패턴의 일관성 정도
     */
    private Integer consistencyScore;

    /**
     * 주기성 감지 여부
     */
    @Builder.Default
    private Boolean cyclicityDetected = false;

    /**
     * 주기성 패턴 (JSON 형태)
     * 예: {"weekly_pattern": {"monday": "low", "friday": "high"}, "cycle_length": 7}
     */
    @Column(columnDefinition = "TEXT")
    private String cyclicityPattern;

    // === 이상치 감지 ===

    /**
     * 이상치 감지 건수
     */
    @Builder.Default
    private Integer anomalyCount = 0;

    /**
     * 이상치 유형별 분포 (JSON 형태)
     * 예: {"duration_anomaly": 3, "efficiency_anomaly": 1, "timing_anomaly": 2}
     */
    @Column(columnDefinition = "TEXT")
    private String anomalyDistribution;

    /**
     * 주요 이상치 날짜들 (JSON 배열)
     * 예: ["2024-01-15", "2024-01-20", "2024-01-25"]
     */
    @Column(columnDefinition = "TEXT")
    private String anomalyDates;

    // === 환경 요인 영향 ===

    /**
     * 날씨 영향도 (-1.0 ~ 1.0)
     * 날씨 변화가 수면에 미치는 영향 정도
     */
    private BigDecimal weatherImpact;

    /**
     * 계절성 영향도 (-1.0 ~ 1.0)
     * 계절 변화가 수면에 미치는 영향 정도
     */
    private BigDecimal seasonalImpact;

    /**
     * 라이프스타일 영향도 (-1.0 ~ 1.0)
     * 생활 패턴 변화가 수면에 미치는 영향 정도
     */
    private BigDecimal lifestyleImpact;

    // === 예측 지표 ===

    /**
     * 다음 기간 예측 점수
     * 현재 트렌드가 지속될 경우의 예상 수면 점수
     */
    private Integer predictedScore;

    /**
     * 예측 신뢰도 (0-100%)
     */
    private Integer predictionConfidence;

    /**
     * 목표 달성 가능성 (%)
     * 사용자 목표 대비 달성 가능성
     */
    private Integer goalAchievabilityPercent;

    // === 비교 분석 ===

    /**
     * 동일 연령대 평균 대비 백분위수
     */
    private Integer ageGroupPercentile;

    /**
     * 동일 성별 평균 대비 백분위수
     */
    private Integer genderGroupPercentile;

    /**
     * 전체 사용자 평균 대비 백분위수
     */
    private Integer overallPercentile;

    // === 메타데이터 ===

    /**
     * 분석 기준 데이터 건수
     */
    private Integer dataPointCount;

    /**
     * 신뢰도 수준 (LOW, MEDIUM, HIGH)
     */
    @Column(length = 10)
    private String confidenceLevel;

    /**
     * 트렌드 계산 시점
     */
    @Builder.Default
    private LocalDateTime calculatedAt = LocalDateTime.now();

    /**
     * 다음 업데이트 예정일
     */
    private LocalDate nextUpdateDate;

    /**
     * 트렌드 분석 버전
     */
    @Column(length = 20)
    @Builder.Default
    private String analysisVersion = "1.0";

    /**
     * 분석 알고리즘 세부정보 (JSON 형태)
     */
    @Column(columnDefinition = "TEXT")
    private String algorithmDetails;

    // === 비즈니스 메서드 ===

    /**
     * 수면 패턴이 안정적인지 확인
     */
    public boolean isStableSleepPattern() {
        return consistencyScore != null && consistencyScore >= 70 &&
               "STABLE".equals(trendDirection);
    }

    /**
     * 수면 개선이 필요한지 확인
     */
    public boolean needsImprovement() {
        return trendScore != null && trendScore < -20 ||
               "DECLINING".equals(trendDirection);
    }

    /**
     * 높은 신뢰도 트렌드인지 확인
     */
    public boolean isHighConfidenceTrend() {
        return "HIGH".equals(confidenceLevel) && 
               dataPointCount != null && dataPointCount >= 14; // 최소 2주 데이터
    }

    /**
     * 이상치가 많은 기간인지 확인
     */
    public boolean hasHighAnomalyRate() {
        if (anomalyCount == null || dataPointCount == null || dataPointCount == 0) {
            return false;
        }
        double anomalyRate = (double) anomalyCount / dataPointCount;
        return anomalyRate > 0.2; // 20% 이상이 이상치
    }

    /**
     * 평균 대비 좋은 성과인지 확인
     */
    public boolean isAboveAveragePerformance() {
        return ageGroupPercentile != null && ageGroupPercentile >= 70;
    }

    /**
     * 계절적 영향을 많이 받는지 확인
     */
    public boolean isSeasonallySensitive() {
        return seasonalImpact != null && 
               seasonalImpact.abs().compareTo(BigDecimal.valueOf(0.3)) > 0;
    }

    /**
     * 트렌드 신뢰도 점수 계산 (1-100)
     */
    public int calculateTrendReliability() {
        int reliabilityScore = 0;

        // 데이터 양 (40점)
        if (dataPointCount != null) {
            if (dataPointCount >= 30) {
                reliabilityScore += 40;
            } else if (dataPointCount >= 14) {
                reliabilityScore += 30;
            } else if (dataPointCount >= 7) {
                reliabilityScore += 20;
            } else {
                reliabilityScore += 10;
            }
        }

        // 일관성 (30점)
        if (consistencyScore != null) {
            reliabilityScore += (consistencyScore * 30) / 100;
        }

        // 최신성 (30점)
        if (calculatedAt != null) {
            long daysOld = java.time.temporal.ChronoUnit.DAYS.between(calculatedAt.toLocalDate(), LocalDate.now());
            if (daysOld <= 1) {
                reliabilityScore += 30;
            } else if (daysOld <= 3) {
                reliabilityScore += 25;
            } else if (daysOld <= 7) {
                reliabilityScore += 20;
            } else {
                reliabilityScore += 10;
            }
        }

        return Math.min(reliabilityScore, 100);
    }
} 