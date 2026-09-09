package com.sleepwell.sleepwell_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * AI 패턴 분석 결과 DTO
 * 
 * 플랫폼에서 제공하는 AI 기반 수면 패턴 분석 결과를 담는 DTO입니다.
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatternAnalysisDto {

    /**
     * 패턴 분석 유형 (SLEEP_CONSISTENCY, CIRCADIAN_RHYTHM, SLEEP_DEBT 등)
     */
    private String patternType;

    /**
     * 패턴 분석 결과 제목
     */
    private String title;

    /**
     * 패턴 분석 상세 설명
     */
    private String description;

    /**
     * 패턴 심각도 (LOW, MEDIUM, HIGH)
     */
    private String severity;

    /**
     * 패턴 신뢰도 점수 (0.0 - 1.0)
     */
    private Double confidence;

    /**
     * 패턴 감지 시간
     */
    private LocalDateTime detectedAt;

    /**
     * 패턴 지속 기간 (일)
     */
    private Integer durationDays;

    /**
     * 개선 권장사항
     */
    private String recommendation;

    /**
     * 패턴 관련 메트릭
     */
    private Map<String, Object> metrics;

    /**
     * 패턴 트렌드 (IMPROVING, STABLE, WORSENING)
     */
    private String trend;

    /**
     * 다음 평가 예정일
     */
    private LocalDateTime nextEvaluationDate;

    /**
     * 패턴 분석 상세 데이터
     */
    private Map<String, Object> analysisDetails;
} 