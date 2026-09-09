package com.sleepwell.sleepwell_backend.entity;

import com.sleepwell.sleepwell_backend.enums.JobStatus;
import com.sleepwell.sleepwell_backend.enums.JobPriority;
import com.sleepwell.sleepwell_backend.enums.WearableSource;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 수면 분석 실행 작업 엔티티
 * 플랫폼 데이터 동기화 및 AI 상담 시스템 트리거를 위한 비동기 작업을 관리합니다.
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "analysis_execution_jobs", indexes = {
    // 사용자별 분석 작업 조회
    @Index(name = "IDX_ANALYSIS_EXECUTION_JOB_USER", columnList = "user_id, createdAt"),
    
    // 우선순위 기반 작업 큐 관리 (핵심)
    @Index(name = "IDX_ANALYSIS_EXECUTION_JOB_QUEUE", columnList = "status, priority, createdAt"),
    
    // 플랫폼 데이터 ID 기반 중복 검사
    @Index(name = "IDX_ANALYSIS_EXECUTION_JOB_PLATFORM", columnList = "platformDataId"),
    
    // 워커별 작업 조회
    @Index(name = "IDX_ANALYSIS_EXECUTION_JOB_WORKER", columnList = "workerId, status"),
    
    // 수면 기록별 분석 작업 조회
    @Index(name = "IDX_ANALYSIS_EXECUTION_JOB_SLEEP_RECORD", columnList = "sleep_record_id")
})
public class AnalysisExecutionJob extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 작업을 요청한 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 연관된 수면 기록 (선택적)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sleep_record_id")
    private SleepRecord sleepRecord;

    /**
     * 플랫폼 소스 (Samsung Health, Apple Health 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WearableSource platformSource;

    /**
     * 플랫폼 데이터 고유 식별자
     */
    @Column(nullable = false, length = 500)
    private String platformDataId;

    /**
     * 작업 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private JobStatus status = JobStatus.QUEUED;

    /**
     * 작업 우선순위
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private JobPriority priority = JobPriority.NORMAL;

    /**
     * 작업 시작 시간
     */
    private LocalDateTime startedAt;

    /**
     * 작업 완료 시간
     */
    private LocalDateTime completedAt;

    /**
     * 처리 시간 (밀리초)
     */
    private Long processingTimeMs;

    /**
     * 작업을 처리하는 워커 ID
     */
    @Column(length = 100)
    private String workerId;

    /**
     * 작업 설정 (JSON 형태)
     */
    @Column(columnDefinition = "TEXT")
    private String jobConfig;

    /**
     * 분석 결과 (JSON 형태)
     */
    @Column(columnDefinition = "TEXT")
    private String analysisResult;

    /**
     * 신뢰도 점수 (0.00 ~ 1.00)
     */
    @Column(precision = 3, scale = 2)
    private BigDecimal confidenceScore;

    /**
     * 데이터 품질 점수 (1-10)
     */
    private Integer dataQualityScore;

    /**
     * AI 상담 트리거 여부
     */
    @Builder.Default
    private Boolean aiConsultationTriggered = false;

    /**
     * 트리거된 상담 세션 ID
     */
    private Long consultationSessionId;

    /**
     * 재시도 횟수
     */
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * 최대 재시도 횟수
     */
    @Builder.Default
    private Integer maxRetries = 3;

    /**
     * 오류 메시지
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 메타데이터 (JSON 형태)
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;
    
    /**
     * 작업 고유 ID (UUID)
     */
    @Column(length = 36)
    private String jobId;
    
    /**
     * 분석 타입
     */
    @Column(length = 50)
    private String analysisType;
    
    /**
     * 대상 날짜
     */
    private LocalDateTime targetDate;
    
    /**
     * 예약된 시간
     */
    private LocalDateTime scheduledAt;
    
    /**
     * 결과 요약
     */
    @Column(columnDefinition = "TEXT")
    private String resultSummary;

    /**
     * 작업을 재시도를 위해 'QUEUED' 상태로 변경하고 재시도 횟수를 증가시킵니다.
     */
    public void markAsQueuedForRetry() {
        if (this.status == JobStatus.FAILED) {
            this.status = JobStatus.QUEUED;
            this.retryCount++;
            this.errorMessage = null; // 재시도 시 이전 오류 메시지 초기화
        } else {
            // 실패 상태가 아닌 작업을 재시도하려고 할 때의 예외 처리 또는 로깅
            // 예를 들어, IllegalStateException을 발생시킬 수 있습니다.
        }
    }
} 