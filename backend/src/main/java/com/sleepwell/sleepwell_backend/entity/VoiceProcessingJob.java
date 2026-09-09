package com.sleepwell.sleepwell_backend.entity;

import com.sleepwell.sleepwell_backend.enums.JobStatus;
import com.sleepwell.sleepwell_backend.enums.ProcessingType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 음성 처리 작업 엔티티
 * STT, TTS, 감정 분석 등의 비동기 음성 처리 작업을 관리합니다.
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "voice_processing_jobs", indexes = {
    // 메시지별 처리 작업 조회
    @Index(name = "IDX_VOICE_PROCESSING_JOB_MESSAGE", columnList = "message_id, createdAt"),
    
    // 우선순위 기반 작업 큐 관리 (핵심)
    @Index(name = "IDX_VOICE_PROCESSING_JOB_QUEUE", columnList = "status, priority, createdAt"),
    @Index(name = "IDX_VOICE_PROCESSING_JOB_QUEUED", columnList = "status, priority, createdAt"),
    
    // 처리 유형별 작업 관리
    @Index(name = "IDX_VOICE_PROCESSING_JOB_TYPE_STATUS", columnList = "processing_type, status, priority"),
    @Index(name = "IDX_VOICE_PROCESSING_JOB_TYPE_QUEUE", columnList = "processing_type, status, createdAt"),
    
    // 워커 관리 및 모니터링
    @Index(name = "IDX_VOICE_PROCESSING_JOB_WORKER", columnList = "worker_id, status, startedAt"),
    @Index(name = "IDX_VOICE_PROCESSING_JOB_PROCESSING", columnList = "status, worker_id, startedAt"),
    
    // 재시도 로직 최적화
    @Index(name = "IDX_VOICE_PROCESSING_JOB_RETRY", columnList = "status, retry_count, max_retries, priority"),
    @Index(name = "IDX_VOICE_PROCESSING_JOB_FAILED", columnList = "status, retry_count, createdAt"),
    
    // AI 모델 성능 분석
    @Index(name = "IDX_VOICE_PROCESSING_JOB_MODEL", columnList = "ai_model, status, confidence_score"),
    @Index(name = "IDX_VOICE_PROCESSING_JOB_CONFIDENCE", columnList = "confidence_score, processing_type, completedAt"),
    
    // 성능 모니터링 및 통계
    @Index(name = "IDX_VOICE_PROCESSING_JOB_PERFORMANCE", columnList = "processing_time_ms, processing_type, status"),
    @Index(name = "IDX_VOICE_PROCESSING_JOB_COMPLETED", columnList = "status, completedAt, processing_time_ms"),
    
    // 높은 우선순위 작업 빠른 조회
    @Index(name = "IDX_VOICE_PROCESSING_JOB_HIGH_PRIORITY", columnList = "priority, status, createdAt"),
    
    // 일별 작업 통계 최적화
    @Index(name = "IDX_VOICE_PROCESSING_JOB_DAILY_STATS", columnList = "createdAt, status, processing_type")
})
public class VoiceProcessingJob extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 연관된 대화 메시지
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false,
                foreignKey = @ForeignKey(name = "FK_VOICE_PROCESSING_JOB_MESSAGE"))
    private ConversationMessage message;

    /**
     * 처리 유형
     * STT, TTS, SENTIMENT_ANALYSIS, INTENT_RECOGNITION
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_type", nullable = false, length = 30)
    private ProcessingType processingType;

    /**
     * 작업 상태
     * QUEUED, PROCESSING, COMPLETED, FAILED, CANCELLED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobStatus status;

    /**
     * 작업 우선순위 (1=최고, 5=최저)
     */
    @Column(name = "priority", nullable = false)
    private Integer priority;

    /**
     * 입력 파일 S3 경로
     */
    @Column(name = "input_path", length = 500)
    private String inputPath;

    /**
     * 출력 파일 S3 경로
     */
    @Column(name = "output_path", length = 500)
    private String outputPath;

    /**
     * 처리 결과 데이터 (JSON)
     */
    @Column(name = "result_data", columnDefinition = "JSON")
    private String resultData;

    /**
     * 처리 오류 메시지
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 처리 시작 시간
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * 처리 완료 시간
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * 처리에 걸린 시간 (밀리초)
     */
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    /**
     * 사용된 AI 모델명
     */
    @Column(name = "ai_model", length = 100)
    private String aiModel;

    /**
     * 모델 신뢰도 점수 (0.0 ~ 1.0)
     */
    @Column(name = "confidence_score")
    private Double confidenceScore;

    /**
     * 재시도 횟수
     */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * 최대 재시도 횟수
     */
    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private Integer maxRetries = 3;

    /**
     * 작업 설정 (JSON)
     * 모델 파라미터, 처리 옵션 등
     */
    @Column(name = "job_config", columnDefinition = "JSON")
    private String jobConfig;

    /**
     * 처리 작업자 ID (워커 식별)
     */
    @Column(name = "worker_id", length = 100)
    private String workerId;
} 