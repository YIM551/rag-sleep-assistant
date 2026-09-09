package com.sleepwell.sleepwell_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

/**
 * 개인 기준선 데이터 DTO
 * 
 * 플랫폼에서 제공하는 개인화된 기준선 데이터를 담는 DTO입니다.
 * 30일 평균 등의 개인별 수면 패턴 기준선을 포함합니다.
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalBaselineDto {

    /**
     * 기준선 계산 시작 날짜
     */
    private LocalDate baselineStartDate;

    /**
     * 기준선 계산 종료 날짜
     */
    private LocalDate baselineEndDate;

    /**
     * 기준선 계산에 사용된 데이터 포인트 수
     */
    private Integer dataPointsCount;

    /**
     * 평균 수면 시간 (분)
     */
    private Double averageSleepDuration;

    /**
     * 평균 수면 효율성 (%)
     */
    private Double averageSleepEfficiency;

    /**
     * 평균 수면 점수
     */
    private Double averageSleepScore;

    /**
     * 평균 깊은 수면 비율 (%)
     */
    private Double averageDeepSleepRatio;

    /**
     * 평균 REM 수면 비율 (%)
     */
    private Double averageRemSleepRatio;

    /**
     * 평균 취침 시간
     */
    private String averageBedTime;

    /**
     * 평균 기상 시간
     */
    private String averageWakeTime;

    /**
     * 수면 일관성 점수 (0-100)
     */
    private Integer consistencyScore;

    /**
     * 추가 기준선 메트릭
     */
    private Map<String, Object> additionalMetrics;
} 