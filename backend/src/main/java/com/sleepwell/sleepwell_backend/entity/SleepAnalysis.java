package com.sleepwell.sleepwell_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 수면 분석 엔티티
 * AI 기반 수면 분석 결과와 개선 권장사항을 저장
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(indexes = {
    // 사용자별 최신 분석 조회 (대시보드 메인 화면)
    @Index(name = "IDX_ANALYSIS_USER_DATE", columnList = "user_id, analysisDate"),
    // 수면 점수 기반 분석 조회 (트렌드 분석)
    @Index(name = "IDX_ANALYSIS_SCORE_DATE", columnList = "sleepScore, analysisDate"),
    // 특정 수면 기록의 분석 결과 조회
    @Index(name = "IDX_ANALYSIS_SLEEP_RECORD", columnList = "sleep_record_id"),
    // 고품질 분석 결과 조회 (신뢰도 기준)
    @Index(name = "IDX_ANALYSIS_CONFIDENCE", columnList = "confidenceScore, analysisDate"),
    // 월간/주간 트렌드 분석용 (기간별 집계)
    @Index(name = "IDX_ANALYSIS_DATE_RANGE", columnList = "analysisDate, user_id")
})
public class SleepAnalysis extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 분석 대상 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, 
                foreignKey = @ForeignKey(name = "FK_SLEEP_ANALYSIS_USER"))
    private User user;

    /**
     * 분석 대상 수면 기록
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sleep_record_id", nullable = false,
                foreignKey = @ForeignKey(name = "FK_SLEEP_ANALYSIS_SLEEP_RECORD"))
    private SleepRecord sleepRecord;

    /**
     * 분석 일자
     */
    @Column(nullable = false)
    private LocalDate analysisDate;

    /**
     * 전체 수면 점수 (0-100)
     */
    @Column(nullable = false)
    private Integer sleepScore;

    /**
     * 수면 품질 레벨 (AI 분석)
     * excellent, good, fair, poor
     */
    @Column(length = 20)
    private String sleepQuality;

    /**
     * AI 분석 결과 전체 점수 (0-100)
     */
    private Integer overallScore;

    /**
     * AI 생성 주요 인사이트 (JSON 배열)
     */
    @Column(columnDefinition = "TEXT")
    private String insights;

    /**
     * AI 분석 상세 내용 (전체 AI 응답)
     */
    @Column(columnDefinition = "TEXT")
    private String details;

    /**
     * AI 생성 여부
     */
    @Builder.Default
    private Boolean aiGenerated = false;

    /**
     * 수면 효율성 (%)
     */
    @Column(nullable = false)
    private Double sleepEfficiency;

    /**
     * 수면 패턴 분석 결과 (JSON 형태)
     * 예: {"deep_sleep_ratio": 0.25, "rem_ratio": 0.20, "light_ratio": 0.55}
     */
    @Column(columnDefinition = "TEXT")
    private String sleepPatternAnalysis;

    /**
     * 환경 요인 분석 (JSON 형태)
     * 예: {"optimal_temperature": "18-22°C", "noise_impact": "medium"}
     */
    @Column(columnDefinition = "TEXT")
    private String environmentalAnalysis;

    /**
     * 수면 방해 요소 분석 (JSON 형태)
     * 예: {"snoring_episodes": 5, "movement_frequency": "high"}
     */
    @Column(columnDefinition = "TEXT")
    private String disruptionAnalysis;

    /**
     * AI 생성 개선 권장사항
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String recommendations;

    /**
     * 주간 트렌드 분석 (JSON 형태)
     */
    @Column(columnDefinition = "TEXT")
    private String weeklyTrends;

    /**
     * 월간 트렌드 분석 (JSON 형태)
     */
    @Column(columnDefinition = "TEXT")
    private String monthlyTrends;

    /**
     * 분석 모델 버전
     */
    @Column(length = 50)
    private String modelVersion;

    /**
     * 신뢰도 점수 (0.0-1.0)
     */
    private BigDecimal confidenceScore;
} 