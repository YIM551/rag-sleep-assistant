package com.sleepwell.sleepwell_backend.repository;

import com.sleepwell.sleepwell_backend.entity.ConversationMessage;
import com.sleepwell.sleepwell_backend.entity.VoiceProcessingJob;
import com.sleepwell.sleepwell_backend.enums.JobStatus;
import com.sleepwell.sleepwell_backend.enums.ProcessingType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 음성 처리 작업 Repository
 * 비동기 작업 큐 관리 및 실시간 모니터링을 위한 최적화된 쿼리 제공
 */
@Repository
public interface VoiceProcessingJobRepository extends JpaRepository<VoiceProcessingJob, Long> {

    /**
     * 메시지별 처리 작업 조회
     */
    @Query("SELECT vpj FROM VoiceProcessingJob vpj WHERE vpj.message = :message ORDER BY vpj.createdAt ASC")
    List<VoiceProcessingJob> findByMessage(@Param("message") ConversationMessage message);

    /**
     * 처리 유형별 작업 조회
     */
    @Query("SELECT vpj FROM VoiceProcessingJob vpj WHERE vpj.processingType = :processingType " +
           "ORDER BY vpj.priority ASC, vpj.createdAt ASC")
    Page<VoiceProcessingJob> findByProcessingType(@Param("processingType") ProcessingType processingType, Pageable pageable);

    /**
     * 작업 상태별 조회
     */
    @Query("SELECT vpj FROM VoiceProcessingJob vpj WHERE vpj.status = :status " +
           "ORDER BY vpj.priority ASC, vpj.createdAt ASC")
    List<VoiceProcessingJob> findByStatus(@Param("status") JobStatus status);

    /**
     * 대기 중인 작업 조회 (우선순위 큐)
     */
    @Query("SELECT vpj FROM VoiceProcessingJob vpj WHERE vpj.status = 'QUEUED' " +
           "ORDER BY vpj.priority ASC, vpj.createdAt ASC")
    List<VoiceProcessingJob> findQueuedJobs();

    /**
     * 처리 중인 작업 조회 (모니터링용)
     */
    @Query("SELECT vpj FROM VoiceProcessingJob vpj WHERE vpj.status = 'PROCESSING' " +
           "ORDER BY vpj.startedAt ASC")
    List<VoiceProcessingJob> findProcessingJobs();

    /**
     * 실패한 작업 조회 (재시도 대상)
     */
    @Query("SELECT vpj FROM VoiceProcessingJob vpj WHERE vpj.status = 'FAILED' " +
           "AND vpj.retryCount < vpj.maxRetries ORDER BY vpj.priority ASC, vpj.createdAt ASC")
    List<VoiceProcessingJob> findRetryableJobs();

    /**
     * 완료된 작업 조회
     */
    @Query("SELECT vpj FROM VoiceProcessingJob vpj WHERE vpj.status = 'COMPLETED' " +
           "ORDER BY vpj.completedAt DESC")
    Page<VoiceProcessingJob> findCompletedJobs(Pageable pageable);

    /**
     * 높은 우선순위 작업 조회 (우선순위 1-2)
     */
    @Query("SELECT vpj FROM VoiceProcessingJob vpj WHERE vpj.priority <= 2 " +
           "AND vpj.status IN ('QUEUED', 'PROCESSING') ORDER BY vpj.priority ASC, vpj.createdAt ASC")
    List<VoiceProcessingJob> findHighPriorityJobs();

    /**
     * 특정 워커가 처리 중인 작업 조회
     */
    @Query("SELECT vpj FROM VoiceProcessingJob vpj WHERE vpj.workerId = :workerId " +
           "AND vpj.status = 'PROCESSING' ORDER BY vpj.startedAt ASC")
    List<VoiceProcessingJob> findByWorker(@Param("workerId") String workerId);

    /**
     * 특정 AI 모델로 처리된 작업 조회
     */
    @Query("SELECT vpj FROM VoiceProcessingJob vpj WHERE vpj.aiModel = :aiModel " +
           "ORDER BY vpj.completedAt DESC")
    Page<VoiceProcessingJob> findByAiModel(@Param("aiModel") String aiModel, Pageable pageable);

    /**
     * 높은 신뢰도 작업 조회 (0.8 이상)
     */
    @Query("SELECT vpj FROM VoiceProcessingJob vpj WHERE vpj.confidenceScore >= 0.8 " +
           "AND vpj.status = 'COMPLETED' ORDER BY vpj.confidenceScore DESC")
    List<VoiceProcessingJob> findHighConfidenceJobs();

    /**
     * 특정 기간 내 생성된 작업 조회
     */
    @Query("SELECT vpj FROM VoiceProcessingJob vpj WHERE vpj.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY vpj.createdAt DESC")
    List<VoiceProcessingJob> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    /**
     * 처리 유형별 작업 개수 조회
     */
    @Query("SELECT COUNT(vpj) FROM VoiceProcessingJob vpj WHERE vpj.processingType = :processingType")
    Long countByProcessingType(@Param("processingType") ProcessingType processingType);

    /**
     * 상태별 작업 개수 조회
     */
    @Query("SELECT COUNT(vpj) FROM VoiceProcessingJob vpj WHERE vpj.status = :status")
    Long countByStatus(@Param("status") JobStatus status);

    /**
     * 처리 유형별 통계
     */
    @Query("SELECT vpj.processingType, COUNT(vpj), AVG(vpj.processingTimeMs), AVG(vpj.confidenceScore) " +
           "FROM VoiceProcessingJob vpj WHERE vpj.status = 'COMPLETED' " +
           "GROUP BY vpj.processingType ORDER BY COUNT(vpj) DESC")
    List<Object[]> getProcessingTypeStatistics();

    /**
     * 상태별 통계
     */
    @Query("SELECT vpj.status, COUNT(vpj) FROM VoiceProcessingJob vpj " +
           "GROUP BY vpj.status ORDER BY COUNT(vpj) DESC")
    List<Object[]> getStatusStatistics();

    /**
     * 워커별 성능 통계
     */
    @Query("SELECT vpj.workerId, COUNT(vpj), AVG(vpj.processingTimeMs) " +
           "FROM VoiceProcessingJob vpj WHERE vpj.status = 'COMPLETED' AND vpj.workerId IS NOT NULL " +
           "GROUP BY vpj.workerId ORDER BY COUNT(vpj) DESC")
    List<Object[]> getWorkerPerformanceStatistics();

    /**
     * AI 모델별 성능 통계
     */
    @Query("SELECT vpj.aiModel, COUNT(vpj), AVG(vpj.confidenceScore), AVG(vpj.processingTimeMs) " +
           "FROM VoiceProcessingJob vpj WHERE vpj.status = 'COMPLETED' AND vpj.aiModel IS NOT NULL " +
           "GROUP BY vpj.aiModel ORDER BY AVG(vpj.confidenceScore) DESC")
    List<Object[]> getAiModelPerformanceStatistics();

    /**
     * 평균 처리 시간 계산
     */
    @Query("SELECT AVG(vpj.processingTimeMs) FROM VoiceProcessingJob vpj " +
           "WHERE vpj.status = 'COMPLETED' AND vpj.processingTimeMs IS NOT NULL")
    Double getAverageProcessingTime();

    /**
     * 평균 신뢰도 점수
     */
    @Query("SELECT AVG(vpj.confidenceScore) FROM VoiceProcessingJob vpj " +
           "WHERE vpj.status = 'COMPLETED' AND vpj.confidenceScore IS NOT NULL")
    Double getAverageConfidenceScore();

    /**
     * 재시도 횟수별 통계
     */
    @Query("SELECT vpj.retryCount, COUNT(vpj) FROM VoiceProcessingJob vpj " +
           "GROUP BY vpj.retryCount ORDER BY vpj.retryCount ASC")
    List<Object[]> getRetryStatistics();

    /**
     * 긴 처리 시간 작업 조회 (10초 이상)
     */
    @Query("SELECT vpj FROM VoiceProcessingJob vpj WHERE vpj.processingTimeMs > 10000 " +
           "AND vpj.status = 'COMPLETED' ORDER BY vpj.processingTimeMs DESC")
    Page<VoiceProcessingJob> findSlowJobs(Pageable pageable);

    /**
     * 작업 상태 업데이트
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE VoiceProcessingJob vpj SET vpj.status = :status, vpj.startedAt = :startedAt, " +
           "vpj.workerId = :workerId WHERE vpj.id = :jobId")
    int updateJobStatus(@Param("jobId") Long jobId, @Param("status") JobStatus status,
                       @Param("startedAt") LocalDateTime startedAt, @Param("workerId") String workerId);

    /**
     * 작업 완료 처리
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE VoiceProcessingJob vpj SET vpj.status = 'COMPLETED', vpj.completedAt = :completedAt, " +
           "vpj.processingTimeMs = :processingTime, vpj.confidenceScore = :confidenceScore, " +
           "vpj.resultData = :resultData WHERE vpj.id = :jobId")
    int completeJob(@Param("jobId") Long jobId, @Param("completedAt") LocalDateTime completedAt,
                   @Param("processingTime") Long processingTime, @Param("confidenceScore") Double confidenceScore,
                   @Param("resultData") String resultData);

    /**
     * 작업 실패 처리
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE VoiceProcessingJob vpj SET vpj.status = 'FAILED', vpj.errorMessage = :errorMessage, " +
           "vpj.retryCount = vpj.retryCount + 1 WHERE vpj.id = :jobId")
    int failJob(@Param("jobId") Long jobId, @Param("errorMessage") String errorMessage);

    /**
     * 일별 작업 통계
     */
    @Query("SELECT CAST(vpj.createdAt AS DATE), COUNT(vpj), " +
           "COUNT(CASE WHEN vpj.status = 'COMPLETED' THEN 1 END), " +
           "COUNT(CASE WHEN vpj.status = 'FAILED' THEN 1 END) " +
           "FROM VoiceProcessingJob vpj " +
           "GROUP BY CAST(vpj.createdAt AS DATE) " +
           "ORDER BY CAST(vpj.createdAt AS DATE) DESC")
    List<Object[]> getDailyJobStatistics();

    /**
     * 최신 작업 조회 (메시지별)
     */
    @Query("SELECT vpj FROM VoiceProcessingJob vpj WHERE vpj.message = :message " +
           "ORDER BY vpj.createdAt DESC LIMIT 1")
    Optional<VoiceProcessingJob> findLatestByMessage(@Param("message") ConversationMessage message);
} 