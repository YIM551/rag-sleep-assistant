package com.sleepwell.sleepwell_backend.repository;

import com.sleepwell.sleepwell_backend.entity.AnalysisExecutionJob;
import com.sleepwell.sleepwell_backend.entity.SleepRecord;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.enums.JobStatus;
import com.sleepwell.sleepwell_backend.enums.WearableSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 수면 분석 실행 작업 Repository
 * 비동기 분석 작업 큐 관리 및 실시간 모니터링을 위한 최적화된 쿼리 제공
 */
@Repository
public interface AnalysisExecutionJobRepository extends JpaRepository<AnalysisExecutionJob, Long> {

    /**
     * 사용자별 분석 작업 조회
     */
    @Query("SELECT aej FROM AnalysisExecutionJob aej WHERE aej.user = :user ORDER BY aej.createdAt DESC")
    List<AnalysisExecutionJob> findByUser(@Param("user") User user);

    /**
     * 사용자별 분석 작업 페이징 조회
     */
    @Query("SELECT aej FROM AnalysisExecutionJob aej WHERE aej.user = :user ORDER BY aej.createdAt DESC")
    Page<AnalysisExecutionJob> findByUser(@Param("user") User user, Pageable pageable);

    /**
     * 수면 기록별 분석 작업 조회
     */
    @Query("SELECT aej FROM AnalysisExecutionJob aej WHERE aej.sleepRecord = :sleepRecord ORDER BY aej.createdAt ASC")
    List<AnalysisExecutionJob> findBySleepRecord(@Param("sleepRecord") SleepRecord sleepRecord);

    /**
     * 작업 상태별 조회
     */
    @Query("SELECT aej FROM AnalysisExecutionJob aej WHERE aej.status = :status " +
           "ORDER BY aej.priority ASC, aej.createdAt ASC")
    List<AnalysisExecutionJob> findByStatus(@Param("status") JobStatus status);

    List<AnalysisExecutionJob> findByStatusOrderByPriorityAsc(JobStatus status);

    List<AnalysisExecutionJob> findByUser_IdOrderByCreatedAtDesc(Long userId);

    long countByStatus(JobStatus status);

    /**
     * 대기 중인 작업 조회 (우선순위 큐)
     */
    @Query("SELECT aej FROM AnalysisExecutionJob aej WHERE aej.status = 'QUEUED' " +
           "ORDER BY aej.priority ASC, aej.createdAt ASC")
    List<AnalysisExecutionJob> findQueuedJobs();

    /**
     * 처리 중인 작업 조회 (모니터링용)
     */
    @Query("SELECT aej FROM AnalysisExecutionJob aej WHERE aej.status = 'PROCESSING' " +
           "ORDER BY aej.startedAt ASC")
    List<AnalysisExecutionJob> findProcessingJobs();

    /**
     * 실패한 작업 조회 (재시도 대상)
     */
    @Query("SELECT aej FROM AnalysisExecutionJob aej WHERE aej.status = 'FAILED' " +
           "AND aej.retryCount < aej.maxRetries ORDER BY aej.priority ASC, aej.createdAt ASC")
    List<AnalysisExecutionJob> findRetryableJobs();

    /**
     * 플랫폼 데이터 ID로 작업 조회
     */
    @Query("SELECT aej FROM AnalysisExecutionJob aej WHERE aej.platformDataId = :platformDataId " +
           "ORDER BY aej.createdAt DESC")
    List<AnalysisExecutionJob> findByPlatformDataId(@Param("platformDataId") String platformDataId);

    /**
     * 작업 상태 업데이트
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE AnalysisExecutionJob aej SET aej.status = :status, aej.startedAt = :startedAt, " +
           "aej.workerId = :workerId WHERE aej.id = :jobId")
    int updateJobStatus(@Param("jobId") Long jobId, @Param("status") JobStatus status,
                       @Param("startedAt") LocalDateTime startedAt, @Param("workerId") String workerId);

    /**
     * 작업 완료 처리
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE AnalysisExecutionJob aej SET aej.status = 'COMPLETED', aej.completedAt = :completedAt, " +
           "aej.processingTimeMs = :processingTime, aej.confidenceScore = :confidenceScore, " +
           "aej.analysisResult = :analysisResult WHERE aej.id = :jobId")
    int completeJob(@Param("jobId") Long jobId, @Param("completedAt") LocalDateTime completedAt,
                   @Param("processingTime") Long processingTime, @Param("confidenceScore") BigDecimal confidenceScore,
                   @Param("analysisResult") String analysisResult);

    /**
     * AI 상담 트리거 업데이트
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE AnalysisExecutionJob aej SET aej.aiConsultationTriggered = true, " +
           "aej.consultationSessionId = :sessionId WHERE aej.id = :jobId")
    int updateAIConsultationTriggered(@Param("jobId") Long jobId, @Param("sessionId") Long sessionId);

    /**
     * 작업 실패 처리 (재시도 가능)
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE AnalysisExecutionJob aej SET aej.status = 'FAILED', aej.errorMessage = :errorMessage, " +
           "aej.retryCount = aej.retryCount + 1 WHERE aej.id = :jobId")
    int failJob(@Param("jobId") Long jobId, @Param("errorMessage") String errorMessage);

    /**
     * 작업 영구 실패 처리 (재시도 불가)
     * retryCount를 maxRetries로 설정하여 재시도를 방지합니다.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE AnalysisExecutionJob aej SET aej.status = 'FAILED', aej.errorMessage = :errorMessage, " +
           "aej.retryCount = aej.maxRetries WHERE aej.id = :jobId")
    int failJobPermanently(@Param("jobId") Long jobId, @Param("errorMessage") String errorMessage);

    /**
     * 최신 작업 조회 (사용자별)
     */
    @Query("SELECT aej FROM AnalysisExecutionJob aej WHERE aej.user = :user " +
           "ORDER BY aej.createdAt DESC LIMIT 1")
    Optional<AnalysisExecutionJob> findLatestByUser(@Param("user") User user);
    
    /**
     * 오래된 완료 작업 조회 (정리 대상)
     */
    @Query("SELECT aej FROM AnalysisExecutionJob aej WHERE aej.status = 'COMPLETED' " +
           "AND aej.completedAt < :cutoffDate ORDER BY aej.completedAt ASC")
    List<AnalysisExecutionJob> findOldCompletedJobs(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * 오래된 완료 작업 삭제
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM AnalysisExecutionJob aej WHERE aej.status = 'COMPLETED' " +
           "AND aej.completedAt < :cutoffDate")
    int deleteOldCompletedJobs(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Admin 기능을 위한 추가 메서드들
    
    /**
     * PENDING 상태 작업 수
     */
    @Query("SELECT COUNT(aej) FROM AnalysisExecutionJob aej WHERE aej.status = 'QUEUED'")
    Long countPendingJobs();
    
    /**
     * PROCESSING 상태 작업 수
     */
    @Query("SELECT COUNT(aej) FROM AnalysisExecutionJob aej WHERE aej.status = 'PROCESSING'")
    Long countProcessingJobs();
} 