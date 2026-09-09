package com.sleepwell.sleepwell_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 수면 트렌드 데이터 DTO
 * 
 * 주간, 월간 수면 패턴 트렌드 데이터를 담는 DTO입니다.
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SleepTrendDto {

    /**
     * 트렌드 분석 시작 날짜
     */
    private LocalDate startDate;

    /**
     * 트렌드 분석 종료 날짜
     */
    private LocalDate endDate;

    /**
     * 트렌드 기간 유형 (WEEKLY, MONTHLY, QUARTERLY)
     */
    private String periodType;

    /**
     * 전체 트렌드 방향 (IMPROVING, STABLE, DECLINING)
     */
    private String overallTrend;

    /**
     * 트렌드 신뢰도 (0.0 - 1.0)
     */
    private Double confidence;

    /**
     * 평균 수면 시간 트렌드 (분)
     */
    private TrendMetric sleepDurationTrend;

    /**
     * 수면 효율성 트렌드 (%)
     */
    private TrendMetric sleepEfficiencyTrend;

    /**
     * 수면 점수 트렌드
     */
    private TrendMetric sleepScoreTrend;

    /**
     * 취침 시간 일관성 트렌드
     */
    private TrendMetric bedtimeConsistencyTrend;

    /**
     * 기상 시간 일관성 트렌드
     */
    private TrendMetric wakeTimeConsistencyTrend;

    /**
     * 주요 트렌드 인사이트
     */
    private List<String> keyInsights;

    /**
     * 개선 권장사항
     */
    private List<String> recommendations;

    /**
     * 트렌드 분석 상세 데이터
     */
    private Map<String, Object> detailedAnalysis;

    /**
     * 다음 분석 예정일
     */
    private LocalDate nextAnalysisDate;

    /**
     * 트렌드 메트릭 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendMetric {
        
        /**
         * 현재 값
         */
        private Double currentValue;

        /**
         * 이전 기간 값
         */
        private Double previousValue;

        /**
         * 변화량 (절대값)
         */
        private Double changeAmount;

        /**
         * 변화율 (%)
         */
        private Double changePercentage;

        /**
         * 트렌드 방향 (UP, DOWN, STABLE)
         */
        private String direction;

        /**
         * 통계적 유의성 여부
         */
        private Boolean isSignificant;

        /**
         * 메트릭 단위
         */
        private String unit;
    }
} 