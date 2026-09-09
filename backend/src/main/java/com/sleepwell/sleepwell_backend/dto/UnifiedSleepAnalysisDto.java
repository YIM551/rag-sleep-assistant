package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.enums.WearableSource;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 통합된 수면 분석 결과 DTO
 * 
 * 플랫폼별 개인화된 수면 데이터를 통합하여
 * AI 상담 시스템에서 활용할 수 있는 형태로 구성된 DTO입니다.
 * 
 * Spring Boot 베스트 프랙티스:
 * - 명확한 데이터 구조와 검증 규칙
 * - Builder 패턴으로 객체 생성 편의성
 * - 적절한 범위 검증과 문서화
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedSleepAnalysisDto {

    /**
     * 원본 데이터 소스
     */
    @NotNull(message = "데이터 소스는 필수입니다")
    private WearableSource originalSource;

    /**
     * 분석 생성 시간
     */
    @NotNull(message = "분석 생성 시간은 필수입니다")
    private LocalDateTime analyzedAt;

    /**
     * 수면 세션 정보
     */
    @NotNull(message = "수면 세션 정보는 필수입니다")
    private SleepSessionInfo sleepSession;

    /**
     * 통합 수면 품질 점수 (0-100)
     */
    @Min(value = 0, message = "수면 점수는 0 이상이어야 합니다")
    @Max(value = 100, message = "수면 점수는 100 이하여야 합니다")
    private Integer unifiedSleepScore;

    /**
     * 플랫폼 제공 점수 (0-100)
     */
    @Min(value = 0, message = "플랫폼 점수는 0 이상이어야 합니다")
    @Max(value = 100, message = "플랫폼 점수는 100 이하여야 합니다")
    private Integer platformScore;

    /**
     * 개인 기준선 대비 평가
     */
    private PersonalBaselineComparison baselineComparison;

    /**
     * 수면 단계별 분석
     */
    private SleepStageAnalysis sleepStages;

    /**
     * 수면 효율성 분석
     */
    private SleepEfficiencyAnalysis efficiency;

    /**
     * AI 패턴 분석 결과
     */
    private List<PatternInsight> patternInsights;

    /**
     * 건강 관련 알림
     */
    private List<HealthAlert> healthAlerts;

    /**
     * 수면 트렌드 분석
     */
    private SleepTrendAnalysis trendAnalysis;

    /**
     * AI 상담을 위한 핵심 인사이트
     */
    private List<String> keyInsights;

    /**
     * 개선 권장사항
     */
    private List<String> recommendations;

    /**
     * 데이터 신뢰도 점수 (0-100)
     */
    @Min(value = 0, message = "신뢰도 점수는 0 이상이어야 합니다")
    @Max(value = 100, message = "신뢰도 점수는 100 이하여야 합니다")
    private Integer reliabilityScore;

    /**
     * 분석에 사용된 데이터 포인트 수
     */
    private Integer dataPointsUsed;

    /**
     * 추가 메타데이터
     */
    private Map<String, Object> additionalMetadata;

    /**
     * 수면 세션 기본 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SleepSessionInfo {
        private LocalDateTime bedTime;
        private LocalDateTime sleepTime;
        private LocalDateTime wakeTime;
        private LocalDateTime outOfBedTime;
        private Integer totalTimeInBed;
        private Integer totalSleepTime;
        private Integer timeToFallAsleep;
        private Integer wakeupCount;
    }

    /**
     * 개인 기준선 비교 분석
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonalBaselineComparison {
        private Double scoreVariation;
        private Double durationVariation;
        private Double efficiencyVariation;
        private String overallTrend;
        private Integer daysFromBaseline;
    }

    /**
     * 수면 단계별 분석
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SleepStageAnalysis {
        private Integer deepSleepMinutes;
        private Integer lightSleepMinutes;
        private Integer remSleepMinutes;
        private Integer awakeMinutes;
        private Double deepSleepPercentage;
        private Double lightSleepPercentage;
        private Double remSleepPercentage;
        private String stageQualityAssessment;
    }

    /**
     * 수면 효율성 분석
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SleepEfficiencyAnalysis {
        private Double efficiency;
        private String efficiencyGrade;
        private Integer timeToFallAsleep;
        private Integer timeAwakeDuringNight;
        private String continuityAssessment;
    }

    /**
     * 패턴 인사이트
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatternInsight {
        private String patternType;
        private String description;
        private String severity;
        private Double confidence;
        private List<String> recommendations;
    }

    /**
     * 건강 알림
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthAlert {
        private String alertType;
        private String severity;
        private String description;
        private LocalDateTime detectedAt;
        private Map<String, Object> details;
    }

    /**
     * 수면 트렌드 분석
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SleepTrendAnalysis {
        private String weeklyTrend;
        private String monthlyTrend;
        private Double averageScore;
        private Double scoreVariability;
        private List<String> trendInsights;
    }
} 