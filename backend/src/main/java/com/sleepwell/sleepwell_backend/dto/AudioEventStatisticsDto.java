package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.enums.AudioEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 오디오 이벤트 통계 DTO
 * 
 * 사용자의 수면 오디오 이벤트에 대한 상세 통계 정보를 제공합니다.
 * 일별, 주별, 월별 트렌드 분석과 이벤트 유형별 상세 분석을 포함합니다.
 * 
 * 주요 통계 지표:
 * - 이벤트 발생 빈도 및 패턴
 * - 강도 분포 및 트렌드
 * - 수면 품질에 미치는 영향
 * - 의료적 중요도 및 권장사항
 * 
 * @author SleepWell Development Team
 * @since 1.0
 * @see AudioEventType
 * @see SleepAudioEventResponseDto
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AudioEventStatisticsDto {

    /**
     * 통계 기간 시작일
     */
    private LocalDate periodStart;

    /**
     * 통계 기간 종료일
     */
    private LocalDate periodEnd;

    /**
     * 총 수면 기록 수
     */
    private Integer totalSleepRecords;

    /**
     * 총 오디오 이벤트 수
     */
    private Long totalAudioEvents;

    // === 전체 통계 ===

    /**
     * 평균 일별 이벤트 수
     */
    private BigDecimal averageDailyEvents;

    /**
     * 평균 이벤트 강도 (1-10)
     */
    private BigDecimal averageIntensity;

    /**
     * 평균 이벤트 지속시간 (초)
     */
    private BigDecimal averageDurationSeconds;

    /**
     * 평균 신뢰도 점수 (0.0-1.0)
     */
    private BigDecimal averageConfidenceScore;

    /**
     * 고강도 이벤트 비율 (%)
     */
    private BigDecimal highIntensityEventRatio;

    /**
     * 의료진 상담 필요 이벤트 비율 (%)
     */
    private BigDecimal medicalAttentionRequiredRatio;

    // === 이벤트 유형별 통계 ===

    /**
     * 이벤트 유형별 발생 횟수
     */
    private Map<AudioEventType, Long> eventCountByType;

    /**
     * 이벤트 유형별 평균 강도
     */
    private Map<AudioEventType, BigDecimal> averageIntensityByType;

    /**
     * 이벤트 유형별 평균 지속시간
     */
    private Map<AudioEventType, BigDecimal> averageDurationByType;

    /**
     * 이벤트 유형별 발생 비율 (%)
     */
    private Map<AudioEventType, BigDecimal> eventRatioByType;

    // === 코골이 특화 통계 ===

    /**
     * 코골이 발생 일수
     */
    private Integer snoringDays;

    /**
     * 코골이 평균 강도
     */
    private BigDecimal snoringAverageIntensity;

    /**
     * 코골이 최대 강도
     */
    private Integer snoringMaxIntensity;

    /**
     * 코골이 시간당 평균 빈도
     */
    private BigDecimal snoringAverageFrequency;

    /**
     * 심한 코골이 비율 (강도 8+ %)
     */
    private BigDecimal severeSnoringRatio;

    // === 이갈이 특화 통계 ===

    /**
     * 이갈이 발생 일수
     */
    private Integer bruxismDays;

    /**
     * 이갈이 평균 강도
     */
    private BigDecimal bruxismAverageIntensity;

    /**
     * 이갈이 최대 강도
     */
    private Integer bruxismMaxIntensity;

    /**
     * 이갈이 시간당 평균 빈도
     */
    private BigDecimal bruxismAverageFrequency;

    /**
     * 심한 이갈이 비율 (강도 7+ %)
     */
    private BigDecimal severeBruxismRatio;

    // === 잠꼬대 특화 통계 ===

    /**
     * 잠꼬대 발생 일수
     */
    private Integer sleepTalkDays;

    /**
     * 잠꼬대 평균 강도
     */
    private BigDecimal sleepTalkAverageIntensity;

    /**
     * 잠꼬대 평균 지속시간
     */
    private BigDecimal sleepTalkAverageDuration;

    /**
     * 잠꼬대 시간당 평균 빈도
     */
    private BigDecimal sleepTalkAverageFrequency;

    // === 환경 소음 특화 통계 ===

    /**
     * 환경 소음 감지 일수
     */
    private Integer environmentalNoiseDays;

    /**
     * 평균 소음 레벨 (dB)
     */
    private BigDecimal averageDecibelLevel;

    /**
     * 최대 소음 레벨 (dB)
     */
    private BigDecimal maxDecibelLevel;

    /**
     * 소음 임계값 초과 비율 (70dB+ %)
     */
    private BigDecimal noiseThresholdExceededRatio;

    // === 트렌드 분석 ===

    /**
     * 일별 이벤트 수 트렌드
     */
    private List<DailyEventTrend> dailyTrends;

    /**
     * 주별 이벤트 수 트렌드
     */
    private List<WeeklyEventTrend> weeklyTrends;

    /**
     * 강도별 분포
     */
    private Map<Integer, Long> intensityDistribution;

    /**
     * 시간대별 이벤트 분포 (0-23시)
     */
    private Map<Integer, Long> hourlyDistribution;

    // === 수면 품질 영향 분석 ===

    /**
     * 오디오 이벤트가 수면 품질에 미치는 평균 영향 점수 (0-100)
     */
    private BigDecimal averageSleepQualityImpact;

    /**
     * 이벤트 발생 시 평균 수면 효율성 저하 (%)
     */
    private BigDecimal sleepEfficiencyReduction;

    /**
     * 이벤트와 깨어남 횟수의 상관관계 점수 (-1 ~ 1)
     */
    private BigDecimal wakeupCorrelationScore;

    // === 의료적 권장사항 ===

    /**
     * 전체 위험도 레벨 (0-4)
     */
    private Integer overallRiskLevel;

    /**
     * 의료진 상담 권장 여부
     */
    private Boolean recommendMedicalConsultation;

    /**
     * 수면 환경 개선 권장 여부
     */
    private Boolean recommendEnvironmentalImprovement;

    /**
     * 생활습관 개선 권장 여부
     */
    private Boolean recommendLifestyleChanges;

    /**
     * 주요 권장사항 목록
     */
    private List<String> keyRecommendations;

    /**
     * 개선 우선순위 이벤트 유형
     */
    private AudioEventType priorityEventType;

    // === 비교 분석 ===

    /**
     * 이전 기간 대비 전체 이벤트 변화율 (%)
     */
    private BigDecimal eventChangeRate;

    /**
     * 이전 기간 대비 평균 강도 변화율 (%)
     */
    private BigDecimal intensityChangeRate;

    /**
     * 개선 여부 (true: 개선됨, false: 악화됨)
     */
    private Boolean isImproving;

    // === 내부 클래스 ===

    /**
     * 일별 이벤트 트렌드
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyEventTrend {
        private LocalDate date;
        private Long eventCount;
        private BigDecimal averageIntensity;
        private Map<AudioEventType, Long> eventsByType;
    }

    /**
     * 주별 이벤트 트렌드
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeeklyEventTrend {
        private LocalDate weekStart;
        private LocalDate weekEnd;
        private Long eventCount;
        private BigDecimal averageIntensity;
        private Map<AudioEventType, Long> eventsByType;
    }

    // === 편의 메서드 ===

    /**
     * 기간 내 평균 일별 이벤트 수 계산
     */
    public BigDecimal calculateAverageDailyEvents() {
        if (totalSleepRecords == null || totalSleepRecords == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(totalAudioEvents)
                .divide(BigDecimal.valueOf(totalSleepRecords), 2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 가장 빈번한 이벤트 유형 반환
     */
    public AudioEventType getMostFrequentEventType() {
        if (eventCountByType == null || eventCountByType.isEmpty()) {
            return null;
        }
        return eventCountByType.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * 가장 강도가 높은 이벤트 유형 반환
     */
    public AudioEventType getHighestIntensityEventType() {
        if (averageIntensityByType == null || averageIntensityByType.isEmpty()) {
            return null;
        }
        return averageIntensityByType.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * 전체 건강 점수 계산 (0-100, 높을수록 좋음)
     */
    public Integer calculateOverallHealthScore() {
        if (averageIntensity == null) return 100;
        
        // 기본 점수에서 강도와 빈도에 따라 차감
        int baseScore = 100;
        
        // 평균 강도에 따른 차감 (강도 1-10 → 차감 0-50점)
        int intensityDeduction = averageIntensity.multiply(BigDecimal.valueOf(5)).intValue();
        
        // 고강도 이벤트 비율에 따른 추가 차감
        int highIntensityDeduction = 0;
        if (highIntensityEventRatio != null) {
            highIntensityDeduction = highIntensityEventRatio.multiply(BigDecimal.valueOf(0.3)).intValue();
        }
        
        // 의료진 상담 필요 비율에 따른 추가 차감
        int medicalDeduction = 0;
        if (medicalAttentionRequiredRatio != null) {
            medicalDeduction = medicalAttentionRequiredRatio.multiply(BigDecimal.valueOf(0.5)).intValue();
        }
        
        int finalScore = baseScore - intensityDeduction - highIntensityDeduction - medicalDeduction;
        return Math.max(0, Math.min(100, finalScore));
    }

    /**
     * 수면 방해 지수 계산 (0-10, 높을수록 방해 심함)
     */
    public BigDecimal calculateSleepDisruptionIndex() {
        if (averageDailyEvents == null || averageIntensity == null) {
            return BigDecimal.ZERO;
        }
        
        // 일별 이벤트 수와 평균 강도를 조합하여 방해 지수 계산
        BigDecimal frequencyFactor = averageDailyEvents.multiply(BigDecimal.valueOf(0.2));
        BigDecimal intensityFactor = averageIntensity.multiply(BigDecimal.valueOf(0.8));
        
        BigDecimal disruptionIndex = frequencyFactor.add(intensityFactor);
        
        // 0-10 범위로 정규화
        return disruptionIndex.min(BigDecimal.valueOf(10)).max(BigDecimal.ZERO);
    }

    /**
     * 개선 추천 우선순위 반환
     */
    public String getImprovementPriority() {
        if (priorityEventType != null) {
            return priorityEventType.getDisplayName() + " 개선이 가장 시급합니다";
        }
        
        if (recommendMedicalConsultation != null && recommendMedicalConsultation) {
            return "의료진 상담을 우선적으로 받으시기 바랍니다";
        }
        
        if (recommendEnvironmentalImprovement != null && recommendEnvironmentalImprovement) {
            return "수면 환경 개선을 우선적으로 고려해보세요";
        }
        
        return "현재 상태를 유지하며 지속적인 모니터링을 권장합니다";
    }
} 