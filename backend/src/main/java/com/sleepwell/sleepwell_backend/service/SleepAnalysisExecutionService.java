package com.sleepwell.sleepwell_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleepwell.sleepwell_backend.dto.PlatformSleepDataDto;
import com.sleepwell.sleepwell_backend.dto.UnifiedSleepAnalysisDto;
import com.sleepwell.sleepwell_backend.entity.AnalysisExecutionJob;
import com.sleepwell.sleepwell_backend.entity.SleepRecord;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.enums.JobStatus;
import com.sleepwell.sleepwell_backend.enums.JobPriority;
import com.sleepwell.sleepwell_backend.enums.WearableSource;
import com.sleepwell.sleepwell_backend.repository.AnalysisExecutionJobRepository;
import com.sleepwell.sleepwell_backend.repository.SleepRecordRepository;
import com.sleepwell.sleepwell_backend.repository.UserRepository;
import com.sleepwell.sleepwell_backend.dto.ConsultationSessionResponseDto;
import com.sleepwell.sleepwell_backend.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 수면 분석 실행 서비스
 * 
 * 플랫폼 데이터 동기화 감지 및 AI 상담 시스템 트리거를 위한
 * 예약 분석 실행 시스템의 핵심 오케스트레이터입니다.
 * 
 * 주요 기능:
 * - 플랫폼 데이터 변경 감지 및 동기화
 * - 비동기 분석 작업 큐 관리
 * - AI 상담 시스템 자동 트리거
 * - 분석 결과 통합 및 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SleepAnalysisExecutionService {

    private final AnalysisExecutionJobRepository analysisExecutionJobRepository;
    private final org.springframework.context.ApplicationContext applicationContext;
    private final PlatformSleepDataIntegrationService platformIntegrationService;
    private final AISleepAnalysisService aiSleepAnalysisService;
    private final SleepRecordRepository sleepRecordRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final ConsultationService consultationService;
    private final NotificationService notificationService;

    private static final String WORKER_ID_PREFIX = "analysis-worker-";
    private static final JobPriority DEFAULT_PRIORITY = JobPriority.NORMAL;
    private static final JobPriority HIGH_PRIORITY = JobPriority.HIGH;

    /**
     * 새로운 플랫폼 데이터에 대한 분석 작업을 스케줄링합니다.
     * 
     * @param userId 사용자 ID
     * @param platformSource 플랫폼 소스
     * @param platformDataId 플랫폼 데이터 ID
     * @param platformData 플랫폼 수면 데이터
     * @param priority 작업 우선순위 (1=최고, 5=최저)
     * @return 생성된 분석 작업
     */
    @Transactional
    public AnalysisExecutionJob scheduleAnalysis(
            @NonNull Long userId,
            @NonNull WearableSource platformSource,
            @NonNull String platformDataId,
            @NonNull PlatformSleepDataDto platformData,
            Integer priority) {
        
        log.info("새로운 수면 분석 작업 스케줄링 - 사용자: {}, 플랫폼: {}, 데이터ID: {}", 
                userId, platformSource, platformDataId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // 중복 작업 확인
        List<AnalysisExecutionJob> existingJobs = analysisExecutionJobRepository
                .findByPlatformDataId(platformDataId);
        
        if (!existingJobs.isEmpty()) {
            log.debug("플랫폼 데이터에 대한 분석 작업이 이미 존재합니다: {}", platformDataId);
            return existingJobs.get(0);
        }

        // 작업 설정 생성
        String jobConfig = createJobConfig(platformData);

        AnalysisExecutionJob job = AnalysisExecutionJob.builder()
                .user(user)
                .platformSource(platformSource)
                .platformDataId(platformDataId)
                .status(JobStatus.QUEUED)
                .priority(priority != null ? JobPriority.fromValue(priority) : DEFAULT_PRIORITY)
                .jobConfig(jobConfig)
                .dataQualityScore(platformData.getDataQualityScore())
                .build();

        AnalysisExecutionJob savedJob = analysisExecutionJobRepository.save(job);
        
        log.info("분석 작업이 스케줄링되었습니다 - 작업ID: {}", savedJob.getId());
        
        // 비동기 처리는 별도의 스케줄러나 워커에서 처리하도록 함
        // processAnalysisJobAsync(savedJob.getId(), platformData);
        
        return savedJob;
    }

    /**
     * 수면 기록에 연관된 분석 작업을 스케줄링합니다.
     * 
     * @param sleepRecord 수면 기록
     * @param platformData 플랫폼 데이터
     * @return 생성된 분석 작업
     */
    @Transactional
    public AnalysisExecutionJob scheduleAnalysisForSleepRecord(
            @NonNull SleepRecord sleepRecord,
            @NonNull PlatformSleepDataDto platformData) {
        
        log.info("수면 기록 연관 분석 작업 스케줄링 - 수면기록ID: {}", sleepRecord.getId());

        String platformDataId = generatePlatformDataId(sleepRecord, platformData);
        
        AnalysisExecutionJob job = AnalysisExecutionJob.builder()
                .user(sleepRecord.getUser())
                .sleepRecord(sleepRecord)
                .platformSource(sleepRecord.getWearableSource())
                .platformDataId(platformDataId)
                .status(JobStatus.QUEUED)
                .priority(HIGH_PRIORITY) // 수면 기록 연관 작업은 높은 우선순위
                .jobConfig(createJobConfig(platformData))
                .dataQualityScore(platformData.getDataQualityScore())
                .build();

        AnalysisExecutionJob savedJob = analysisExecutionJobRepository.save(job);
        
        log.info("수면 기록 분석 작업이 스케줄링되었습니다 - 작업ID: {}", savedJob.getId());
        
        // 비동기 처리는 별도의 스케줄러나 워커에서 처리하도록 함
        // processAnalysisJobAsync(savedJob.getId(), platformData);
        
        return savedJob;
    }

    /**
     * 분석 작업을 비동기로 처리합니다.
     *
     * @param jobId 작업 ID
     * @param platformData 플랫폼 데이터
     */
    @Async("analysisTaskExecutor")
    public CompletableFuture<Void> processAnalysisJobAsync(Long jobId, PlatformSleepDataDto platformData) {
        String workerId = WORKER_ID_PREFIX + UUID.randomUUID().toString().substring(0, 8);

        // 자기 자신의 프록시를 가져옴 (트랜잭션 적용을 위해)
        SleepAnalysisExecutionService self = applicationContext.getBean(SleepAnalysisExecutionService.class);

        try {
            log.info("비동기 분석 처리 시작 - 작업ID: {}, 워커ID: {}", jobId, workerId);

            // 작업 상태를 PROCESSING으로 업데이트
            self.updateJobStatusToProcessing(jobId, workerId);

            // 실제 분석 처리 수행
            self.processAnalysisJobWithTransaction(jobId, platformData, workerId);

            log.info("비동기 분석 처리 완료 - 작업ID: {}", jobId);

        } catch (IllegalArgumentException | IllegalStateException | NullPointerException e) {
            // 영구적 실패 (데이터 문제, 설정 오류 등) - 재시도하지 않음
            log.error("비동기 분석 처리 영구 실패 (재시도 안함) - 작업ID: {}, 오류: {}",
                jobId, e.getClass().getSimpleName(), e);
            self.markJobAsPermanentlyFailed(jobId, "영구 실패: " + e.getMessage());

        } catch (Exception e) {
            // 일시적 실패 (네트워크 오류, 외부 API 오류 등) - 재시도 가능
            log.error("비동기 분석 처리 일시 실패 (재시도 가능) - 작업ID: {}", jobId, e);
            self.markJobAsFailed(jobId, e.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 작업 상태를 PROCESSING으로 업데이트 (별도 트랜잭션)
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void updateJobStatusToProcessing(Long jobId, String workerId) {
        analysisExecutionJobRepository.updateJobStatus(
                jobId, JobStatus.PROCESSING, LocalDateTime.now(), workerId);
    }

    /**
     * 작업을 영구 실패로 표시 (별도 트랜잭션)
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void markJobAsPermanentlyFailed(Long jobId, String errorMessage) {
        analysisExecutionJobRepository.failJobPermanently(jobId, errorMessage);
    }

    /**
     * 작업을 실패로 표시 (별도 트랜잭션)
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void markJobAsFailed(Long jobId, String errorMessage) {
        analysisExecutionJobRepository.failJob(jobId, errorMessage);
    }

    /**
     * 트랜잭션 내에서 분석 작업 처리
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void processAnalysisJobWithTransaction(Long jobId, PlatformSleepDataDto platformData, String workerId) throws Exception {
        processAnalysisJob(jobId, platformData, workerId);
    }

    /**
     * 분석 작업의 핵심 처리 로직을 수행합니다.
     *
     * @param jobId 작업 ID
     * @param platformData 플랫폼 데이터
     * @param workerId 워커 ID
     * @throws Exception 분석 처리 중 발생한 모든 예외
     */
    private void processAnalysisJob(Long jobId, PlatformSleepDataDto platformData, String workerId) throws Exception {
        long startTime = System.currentTimeMillis();

        try {
            Optional<AnalysisExecutionJob> jobOpt = analysisExecutionJobRepository.findById(jobId);
            if (jobOpt.isEmpty()) {
                log.error("분석 작업을 찾을 수 없습니다: {}", jobId);
                return;
            }

            AnalysisExecutionJob job = jobOpt.get();

            log.debug("분석 작업 처리 중 - 작업ID: {}, 사용자ID: {}, 플랫폼: {}",
                    jobId, job.getUser().getId(), job.getPlatformSource());

            // platformData가 null인 경우 (Scheduler에서 호출된 경우) DB에서 복원 시도
            if (platformData == null && job.getJobConfig() != null) {
                log.info("DB에서 플랫폼 데이터 복원 시도 - 작업ID: {}", jobId);
                platformData = deserializePlatformData(job.getJobConfig());

                if (platformData == null) {
                    log.error("플랫폼 데이터 복원 실패 - 작업ID: {}", jobId);
                    throw new IllegalStateException("플랫폼 데이터를 복원할 수 없습니다");
                }

                log.info("플랫폼 데이터 복원 성공 - 작업ID: {}", jobId);
            }

            // MANUAL 소스는 분석을 건너뜀 (플랫폼 어댑터가 없음)
            if (job.getPlatformSource() == WearableSource.MANUAL) {
                log.info("MANUAL 소스는 분석을 건너뜁니다 - 작업ID: {}", jobId);
                analysisExecutionJobRepository.completeJob(
                    jobId,
                    LocalDateTime.now(),
                    System.currentTimeMillis() - startTime,
                    null,
                    "{\"skipped\": true, \"reason\": \"MANUAL source does not require analysis\"}");
                return;
            }

            // platformData가 여전히 null이면 예외 발생
            if (platformData == null) {
                throw new IllegalArgumentException("플랫폼 데이터가 null입니다");
            }

            // 1. 플랫폼 데이터 통합 분석 수행
            UnifiedSleepAnalysisDto unifiedAnalysis = platformIntegrationService
                    .integrateAndAnalyze(platformData, job.getPlatformSource());

            // 2. 데이터 품질 평가
            int dataQuality = platformIntegrationService
                    .calculateDataQuality(platformData, job.getPlatformSource());

            // 3. AI 상담 트리거 조건 확인 및 실행
            boolean aiTriggered = checkAndTriggerAIConsultation(job, unifiedAnalysis, platformData);

            // 4. 분석 결과 저장
            String analysisResultJson = objectMapper.writeValueAsString(unifiedAnalysis);
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            // 5. 작업 완료 처리
            analysisExecutionJobRepository.completeJob(
                    jobId, 
                    LocalDateTime.now(), 
                    processingTime, 
                    unifiedAnalysis.getReliabilityScore() != null ? 
                            BigDecimal.valueOf(unifiedAnalysis.getReliabilityScore() / 100.0) : null,
                    analysisResultJson);

            log.info("분석 작업이 성공적으로 완료되었습니다 - 작업ID: {}, 처리시간: {}ms", jobId, processingTime);

            // 6. 분석 완료 알림 발송
            sendAnalysisCompletionNotification(job, unifiedAnalysis, processingTime);

        } catch (Exception e) {
            log.error("분석 작업 처리 실패 - 작업ID: {}", jobId, e);
            // 예외를 상위로 전파하여 processAnalysisJobAsync에서 분류 처리
            throw e;
        }
    }

    /**
     * AI 상담 트리거 조건을 확인하고 필요시 상담을 시작합니다.
     * 
     * @param job 분석 작업
     * @param unifiedAnalysis 통합 분석 결과
     * @param platformData 플랫폼 데이터
     * @return AI 상담 트리거 여부
     */
    private boolean checkAndTriggerAIConsultation(
            AnalysisExecutionJob job, 
            UnifiedSleepAnalysisDto unifiedAnalysis, 
            PlatformSleepDataDto platformData) {
        
        try {
            // AI 상담 트리거 조건 확인
            if (shouldTriggerAIConsultation(unifiedAnalysis, platformData)) {
                log.info("AI 상담 트리거 조건 만족 - 작업ID: {}", job.getId());
                
                // 분석 데이터를 JSON 문자열로 변환
                String analysisDataJson = createAnalysisDataJson(unifiedAnalysis, platformData);
                
                // AI 상담 세션 생성
                try {
                    ConsultationSessionResponseDto sessionResponse = consultationService.createAutomaticSession(
                        job.getUser(), analysisDataJson);
                    
                    Long sessionId = sessionResponse.getSessionId();
                    analysisExecutionJobRepository.updateAIConsultationTriggered(job.getId(), sessionId);
                    
                    log.info("AI 상담이 성공적으로 트리거되었습니다 - 작업ID: {}, 세션ID: {}", 
                            job.getId(), sessionId);
                } catch (Exception e) {
                    log.error("AI 상담 세션 생성 실패 - 작업ID: {}", job.getId(), e);
                    // AI 상담 실패는 분석 작업 자체의 실패로 처리하지 않음
                }
                
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("AI 상담 트리거 실패 - 작업ID: {}", job.getId(), e);
            return false;
        }
    }

    /**
     * AI 상담 트리거가 필요한지 결정하는 내부 로직
     */
    private boolean shouldTriggerAIConsultation(
            UnifiedSleepAnalysisDto unifiedAnalysis, 
            PlatformSleepDataDto platformData) {
        // 데이터 품질이 낮으면 트리거하지 않음
        if (platformData.getDataQualityScore() < 60) return false;

        // 분석 결과의 신뢰도가 낮으면 트리거하지 않음
        if (unifiedAnalysis.getReliabilityScore() < 70) return false;

        // 심각한 수면 이상 징후가 있을 경우 트리거
        return hasSignificantFindings(unifiedAnalysis);
    }
    
    /**
     * 분석 결과에 중요한 발견사항이 있는지 확인합니다.
     * 
     * @param unifiedAnalysis 통합 분석 결과
     * @return 중요한 발견사항 존재 여부
     */
    private boolean hasSignificantFindings(UnifiedSleepAnalysisDto unifiedAnalysis) {
        // 키 인사이트가 있는 경우
        if (unifiedAnalysis.getKeyInsights() != null &&
            !unifiedAnalysis.getKeyInsights().isEmpty()) {
            return true;
        }
        
        // 권장사항이 있는 경우
        if (unifiedAnalysis.getRecommendations() != null &&
            !unifiedAnalysis.getRecommendations().isEmpty()) {
            return true;
        }
        
        return false;
    }

    /**
     * 대기 중인 분석 작업들을 조회합니다.
     * 
     * @return 대기 중인 작업 목록
     */
    public List<AnalysisExecutionJob> getQueuedJobs() {
        return analysisExecutionJobRepository.findByStatusOrderByPriorityAsc(JobStatus.QUEUED);
    }

    /**
     * 처리 중인 분석 작업들을 조회합니다.
     * 
     * @return 처리 중인 작업 목록
     */
    public List<AnalysisExecutionJob> getProcessingJobs() {
        return analysisExecutionJobRepository.findByStatus(JobStatus.PROCESSING);
    }

    /**
     * 재시도 가능한 실패 작업들을 조회합니다.
     * 
     * @return 재시도 가능한 작업 목록
     */
    public List<AnalysisExecutionJob> getRetryableJobs() {
        return analysisExecutionJobRepository.findByStatus(JobStatus.FAILED);
    }

    /**
     * 사용자의 분석 작업 히스토리를 조회합니다.
     * 
     * @param userId 사용자 ID
     * @return 사용자의 분석 작업 목록
     */
    public List<AnalysisExecutionJob> getUserAnalysisHistory(Long userId) {
        return analysisExecutionJobRepository.findByUser_IdOrderByCreatedAtDesc(userId);
    }

    /**
     * 특정 분석 작업을 ID로 조회합니다.
     * 
     * @param jobId 작업 ID
     * @return 분석 작업 (있는 경우)
     */
    public Optional<AnalysisExecutionJob> getAnalysisJobById(Long jobId) {
        return analysisExecutionJobRepository.findById(jobId);
    }

    /**
     * 특정 분석 작업을 조회하며, 요청한 사용자가 소유자인지 확인합니다.
     *
     * @param jobId 조회할 작업 ID
     * @param user 요청한 사용자
     * @return 분석 작업 엔티티
     * @throws SecurityException 사용자가 작업에 대한 권한이 없을 경우
     */
    public AnalysisExecutionJob getAnalysisJob(Long jobId, User user) {
        AnalysisExecutionJob job = analysisExecutionJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("ID " + jobId + "에 해당하는 작업을 찾을 수 없습니다."));

        if (!job.getUser().getId().equals(user.getId())) {
            throw new SecurityException("이 작업에 접근할 권한이 없습니다.");
        }
        return job;
    }

    /**
     * 특정 분석 작업을 조회하며, 요청한 사용자 ID가 소유자인지 확인합니다.
     *
     * @param jobId 조회할 작업 ID
     * @param userId 요청한 사용자 ID
     * @return 분석 작업 엔티티
     * @throws SecurityException 사용자가 작업에 대한 권한이 없을 경우
     */
    public AnalysisExecutionJob getAnalysisJobByIdAndUserId(Long jobId, Long userId) {
        AnalysisExecutionJob job = analysisExecutionJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("ID " + jobId + "에 해당하는 작업을 찾을 수 없습니다."));

        if (!job.getUser().getId().equals(userId)) {
            throw new SecurityException("이 작업에 접근할 권한이 없습니다.");
        }
        return job;
    }

    /**
     * 현재 작업 큐의 상태를 요약하여 반환합니다.
     *
     * @return 큐 상태 요약 문자열
     */
    public String getQueueStatus() {
        long queuedCount = analysisExecutionJobRepository.countByStatus(JobStatus.QUEUED);
        long processingCount = analysisExecutionJobRepository.countByStatus(JobStatus.PROCESSING);
        return String.format("Queue Status: %d queued, %d processing.", queuedCount, processingCount);
    }
    
    /**
     * 사용자 이름(이메일)으로 사용자 ID를 조회합니다.
     *
     * @param username 사용자 이름(이메일)
     * @return 사용자 ID
     * @throws IllegalArgumentException 해당 이름의 사용자가 없을 경우
     */
    public Long getUserIdByUsername(String username) {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username))
                .getId();
    }

    /**
     * 플랫폼 데이터를 JSON 문자열로 직렬화합니다.
     *
     * @param platformData 플랫폼 데이터
     * @return JSON 형태의 플랫폼 데이터 (실패 시 null)
     */
    private String serializePlatformData(PlatformSleepDataDto platformData) {
        if (platformData == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(platformData);
        } catch (Exception e) {
            log.error("플랫폼 데이터 직렬화 실패", e);
            return null;
        }
    }

    /**
     * JSON 문자열을 플랫폼 데이터로 역직렬화합니다.
     *
     * @param platformDataJson JSON 형태의 플랫폼 데이터
     * @return 플랫폼 데이터 객체 (실패 시 null)
     */
    private PlatformSleepDataDto deserializePlatformData(String platformDataJson) {
        if (platformDataJson == null || platformDataJson.trim().isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readValue(platformDataJson, PlatformSleepDataDto.class);
        } catch (Exception e) {
            log.error("플랫폼 데이터 역직렬화 실패", e);
            return null;
        }
    }

    /**
     * 작업 설정을 JSON 형태로 생성합니다.
     *
     * @param platformData 플랫폼 데이터
     * @return JSON 형태의 작업 설정
     */
    private String createJobConfig(PlatformSleepDataDto platformData) {
        // platformData 전체를 직렬화하여 저장
        return serializePlatformData(platformData);
    }

    /**
     * 플랫폼 데이터 ID를 생성합니다.
     * 
     * @param sleepRecord 수면 기록
     * @param platformData 플랫폼 데이터
     * @return 플랫폼 데이터 ID
     */
    private String generatePlatformDataId(SleepRecord sleepRecord, PlatformSleepDataDto platformData) {
        return String.format("%s_%s_%s", 
                sleepRecord.getWearableSource().name(),
                sleepRecord.getId(),
                platformData.getCollectedAt().toLocalDate());
    }
    
    /**
     * 분석 데이터를 AI 상담용 JSON 문자열로 변환
     */
    private String createAnalysisDataJson(UnifiedSleepAnalysisDto unifiedAnalysis, 
                                         PlatformSleepDataDto platformData) {
        try {
            Map<String, Object> analysisData = Map.of(
                "analysisTime", LocalDateTime.now().toString(),
                "unifiedSleepScore", unifiedAnalysis.getUnifiedSleepScore() != null ? unifiedAnalysis.getUnifiedSleepScore() : 0,
                "platformScore", unifiedAnalysis.getPlatformScore() != null ? unifiedAnalysis.getPlatformScore() : 0,
                "totalSleepTime", unifiedAnalysis.getSleepSession() != null ? unifiedAnalysis.getSleepSession().getTotalSleepTime() : 0,
                "wakeupCount", unifiedAnalysis.getSleepSession() != null ? unifiedAnalysis.getSleepSession().getWakeupCount() : 0,
                "keyInsights", unifiedAnalysis.getKeyInsights() != null ? unifiedAnalysis.getKeyInsights() : List.of(),
                "recommendations", unifiedAnalysis.getRecommendations() != null ? unifiedAnalysis.getRecommendations() : List.of(),
                "source", platformData.getSource()
            );
            
            return objectMapper.writeValueAsString(analysisData);
        } catch (Exception e) {
            log.error("분석 데이터 JSON 변환 실패", e);
            return "{}";
        }
    }
    
    /**
     * 실패한 분석 작업들을 재시도합니다.
     * 
     * @return 재시도된 작업의 수
     */
    @Transactional
    public int retryFailedJobs() {
        List<AnalysisExecutionJob> retryableJobs = getRetryableJobs();
        int retryCount = 0;
        int skippedCount = 0;

        for (AnalysisExecutionJob job : retryableJobs) {
            try {
                // 재시도 횟수가 최대값에 도달한 작업은 건너뜀 (영구 실패)
                if (job.getRetryCount() >= job.getMaxRetries()) {
                    log.warn("재시도 횟수 초과로 건너뜀 - 작업ID: {}, 재시도: {}/{}",
                        job.getId(), job.getRetryCount(), job.getMaxRetries());
                    skippedCount++;
                    continue;
                }

                // 작업 상태를 QUEUED로 변경하고 재시도 횟수 증가
                job.markAsQueuedForRetry();
                analysisExecutionJobRepository.save(job);
                retryCount++;

                log.info("분석 작업 재시도 시작 - 작업ID: {}, 재시도 횟수: {}/{}",
                        job.getId(), job.getRetryCount(), job.getMaxRetries());

            } catch (Exception e) {
                log.error("분석 작업 재시도 실패 - 작업ID: {}", job.getId(), e);
            }
        }

        log.info("실패한 작업 재시도 완료 - 총 {}개 작업 재시도됨, {}개 작업 건너뜀", retryCount, skippedCount);
        return retryCount;
    }
    
    /**
     * 오래된 완료 작업들을 정리합니다.
     * 
     * @param retentionDays 보관 기간 (일)
     * @return 삭제된 작업의 수
     */
    @Transactional
    public int cleanupOldCompletedJobs(int retentionDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        
        log.info("오래된 완료 작업 정리 시작 - {}일 이상 된 작업 삭제", retentionDays);
        
        // 삭제 전 대상 작업 확인
        List<AnalysisExecutionJob> jobsToDelete = analysisExecutionJobRepository.findOldCompletedJobs(cutoffDate);
        log.info("삭제 대상 작업 수: {}개", jobsToDelete.size());
        
        // 실제 삭제 수행
        int deletedCount = analysisExecutionJobRepository.deleteOldCompletedJobs(cutoffDate);
        
        log.info("오래된 완료 작업 정리 완료 - 총 {}개 작업 삭제됨", deletedCount);
        return deletedCount;
    }

    /**
     * 분석 완료 알림을 발송합니다.
     * 
     * @param job 완료된 분석 작업
     * @param unifiedAnalysis 통합 분석 결과
     * @param processingTime 처리 시간 (밀리초)
     */
    private void sendAnalysisCompletionNotification(AnalysisExecutionJob job, UnifiedSleepAnalysisDto unifiedAnalysis, long processingTime) {
        try {
            User user = job.getUser();
            
            // 알림 제목과 내용 구성
            String title = "수면 분석이 완료되었습니다";
            
            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append("최신 수면 데이터 분석이 완료되었습니다.\n\n");
            
            // 분석 결과 요약 추가
            if (unifiedAnalysis != null) {
                if (unifiedAnalysis.getUnifiedSleepScore() != null) {
                    contentBuilder.append(String.format("🌙 수면 품질 점수: %d점\n", 
                        unifiedAnalysis.getUnifiedSleepScore()));
                }
                
                if (unifiedAnalysis.getReliabilityScore() != null) {
                    contentBuilder.append(String.format("📊 데이터 신뢰도: %d%%\n", 
                        unifiedAnalysis.getReliabilityScore()));
                }
                
                if (unifiedAnalysis.getSleepSession() != null && unifiedAnalysis.getSleepSession().getTotalSleepTime() != null && unifiedAnalysis.getSleepSession().getTotalSleepTime() > 0) {
                    int totalMinutes = unifiedAnalysis.getSleepSession().getTotalSleepTime();
                    int hours = totalMinutes / 60;
                    int minutes = totalMinutes % 60;
                    contentBuilder.append(String.format("💤 총 수면 시간: %d시간 %d분\n", hours, minutes));
                }
            }
            
            contentBuilder.append("\n앱에서 자세한 분석 결과를 확인해보세요!");
            
            String content = contentBuilder.toString();
            
            // 분석 완료 알림 발송 (관련 데이터로 분석 작업 ID 저장)
            notificationService.createRelatedNotification(
                user.getId(),
                NotificationType.SLEEP_ANALYSIS_COMPLETE,
                title,
                content,
                job.getId(),
                "AnalysisExecutionJob",
                null // 기본 우선순위 사용
            );
            
            log.info("분석 완료 알림 발송 완료 - 사용자ID: {}, 작업ID: {}", 
                user.getId(), job.getId());
                
        } catch (Exception e) {
            log.error("분석 완료 알림 발송 실패 - 작업ID: {}", job.getId(), e);
            // 알림 실패가 분석 작업 자체를 실패로 만들지 않도록 예외를 던지지 않음
        }
    }
} 