package com.sleepwell.sleepwell_backend.scheduler;

import com.sleepwell.sleepwell_backend.entity.AnalysisExecutionJob;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.repository.UserRepository;
import com.sleepwell.sleepwell_backend.repository.SleepRecordRepository;
import com.sleepwell.sleepwell_backend.service.SleepAnalysisExecutionService;
import com.sleepwell.sleepwell_backend.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 수면 분석 실행 스케줄러
 * 
 * 예약된 분석 작업들을 주기적으로 처리하고 
 * 실패한 작업들의 재시도를 관리하는 스케줄러입니다.
 * 
 * Spring Boot 베스트 프랙티스:
 * - @Scheduled 어노테이션으로 주기적 실행
 * - @ConditionalOnProperty로 환경별 활성화 제어
 * - 적절한 로깅과 예외 처리
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    value = "sleepwell.scheduler.analysis-execution.enabled", 
    havingValue = "true", 
    matchIfMissing = true
)
public class SleepAnalysisScheduler {

    private final SleepAnalysisExecutionService analysisExecutionService;
    private final UserRepository userRepository;
    private final SleepRecordRepository sleepRecordRepository;
    private final MetricsService metricsService;

    /**
     * 대기 중인 분석 작업들을 주기적으로 처리합니다.
     * 1분마다 실행되어 대기 중인 작업을 실행합니다.
     */
    @Scheduled(fixedRate = 60000) // 1분 = 60,000ms
    public void processQueuedJobs() {
        try {
            List<AnalysisExecutionJob> queuedJobs = analysisExecutionService.getQueuedJobs();

            if (!queuedJobs.isEmpty()) {
                log.info("대기 중인 분석 작업 처리 시작: {} 개", queuedJobs.size());

                // 최대 5개씩 처리 (부하 관리)
                int processCount = Math.min(queuedJobs.size(), 5);
                for (int i = 0; i < processCount; i++) {
                    AnalysisExecutionJob job = queuedJobs.get(i);
                    try {
                        log.info("분석 작업 처리 시작 - 작업ID: {}, sleepRecord: {}",
                                job.getId(), job.getSleepRecord() != null ? job.getSleepRecord().getId() : "null");

                        // 비동기로 분석 작업 처리
                        analysisExecutionService.processAnalysisJobAsync(job.getId(), null);

                    } catch (Exception e) {
                        log.error("분석 작업 처리 실패 - 작업ID: {}", job.getId(), e);
                    }
                }

                log.info("분석 작업 처리 시작 완료: {} 개 작업 실행됨", processCount);
            }

        } catch (Exception e) {
            log.error("대기 중인 분석 작업 처리 실패", e);
        }
    }

    /**
     * 대기 중인 분석 작업들을 주기적으로 모니터링합니다.
     * 5분마다 실행되어 작업 큐 상태를 확인합니다.
     */
    @Scheduled(fixedRate = 300000) // 5분 = 300,000ms
    public void monitorQueuedJobs() {
        try {
            List<AnalysisExecutionJob> queuedJobs = analysisExecutionService.getQueuedJobs();
            List<AnalysisExecutionJob> processingJobs = analysisExecutionService.getProcessingJobs();
            
            if (!queuedJobs.isEmpty() || !processingJobs.isEmpty()) {
                log.info("분석 작업 큐 상태 - 대기: {}, 처리중: {}", 
                        queuedJobs.size(), processingJobs.size());
                
                // 대기 중인 작업이 많은 경우 경고
                if (queuedJobs.size() > 10) {
                    log.warn("대기 중인 분석 작업이 많습니다: {} 개", queuedJobs.size());
                }
                
                // 처리 시간이 오래 걸리는 작업 확인
                processingJobs.forEach(job -> {
                    if (job.getStartedAt() != null) {
                        long processingMinutes = java.time.Duration.between(
                                job.getStartedAt(), 
                                java.time.LocalDateTime.now()
                        ).toMinutes();
                        
                        if (processingMinutes > 30) { // 30분 이상 처리 중인 경우
                            log.warn("장시간 처리 중인 분석 작업 발견 - 작업ID: {}, 처리시간: {}분", 
                                    job.getId(), processingMinutes);
                        }
                    }
                });
            }
            
        } catch (Exception e) {
            log.error("분석 작업 큐 모니터링 실패", e);
        }
    }

    /**
     * 실패한 분석 작업들의 재시도를 주기적으로 수행합니다.
     * 10분마다 실행되어 재시도 가능한 작업들을 처리합니다.
     */
    @Scheduled(fixedRate = 600000) // 10분 = 600,000ms
    public void retryFailedJobs() {
        try {
            log.info("실패한 분석 작업 재시도 스케줄 실행");

            // retryFailedJobs가 모든 실패 작업을 한번에 처리
            int retriedCount = analysisExecutionService.retryFailedJobs();

            if (retriedCount > 0) {
                log.info("실패 작업 재시도 스케줄 완료 - 재시도됨: {} 개", retriedCount);
            }

        } catch (Exception e) {
            log.error("실패 작업 재시도 스케줄링 실패", e);
        }
    }

    /**
     * 완료된 분석 작업들의 정리를 주기적으로 수행합니다.
     * 매일 새벽 2시에 실행되어 오래된 완료 작업들을 정리합니다.
     */
    @Scheduled(cron = "0 0 2 * * *") // 매일 새벽 2시
    public void cleanupCompletedJobs() {
        try {
            log.info("완료된 분석 작업 정리 시작");
            
            // 30일 이상 된 완료 작업 삭제
            int deletedCount = analysisExecutionService.cleanupOldCompletedJobs(30);
            log.info("오래된 완료 작업 {} 개 삭제됨", deletedCount);
            
            log.info("완료된 분석 작업 정리 완료");
            
        } catch (Exception e) {
            log.error("완료된 분석 작업 정리 실패", e);
        }
    }

    /**
     * 플랫폼 데이터 동기화를 주기적으로 확인합니다.
     * 30분마다 실행되어 새로운 플랫폼 데이터가 있는지 확인합니다.
     */
    @Scheduled(fixedRate = 1800000) // 30분 = 1,800,000ms
    public void checkPlatformDataSync() {
        try {
            log.debug("플랫폼 데이터 동기화 상태 확인 시작");
            
            // 모든 활성 사용자의 플랫폼 데이터 동기화 확인
            List<User> activeUsers = userRepository.findAllByActiveTrue();
            int syncCheckCount = 0;
            
            for (User user : activeUsers) {
                try {
                    // 사용자의 최근 수면 기록 확인
                    LocalDate lastRecordDate = sleepRecordRepository
                        .findFirstByUserIdOrderByCreatedAtDesc(user.getId())
                        .map(record -> record.getRecordDate())
                        .orElse(LocalDate.now().minusDays(7));
                    
                    // 최근 7일 이내에 기록이 없는 경우 동기화 필요 플래그
                    LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
                    if (lastRecordDate.isBefore(sevenDaysAgo)) {
                        log.info("사용자 {}의 수면 기록이 7일 이상 없음. 마지막 기록: {}", 
                            user.getId(), lastRecordDate);
                        syncCheckCount++;
                        
                        // 플랫폼 동기화가 필요한 사용자로 표시
                        // 실제 동기화는 별도 서비스에서 처리
                        metricsService.incrementPlatformSyncSuccess("SYNC_CHECK");
                    }
                } catch (Exception e) {
                    log.error("사용자 {} 플랫폼 데이터 동기화 확인 실패", user.getId(), e);
                    metricsService.incrementPlatformSyncFailed("SYNC_CHECK");
                }
            }
            
            if (syncCheckCount > 0) {
                log.info("플랫폼 데이터 동기화 확인 완료: {}명의 사용자가 동기화 필요", syncCheckCount);
            }
            
            log.debug("플랫폼 데이터 동기화 상태 확인 완료");
            
        } catch (Exception e) {
            log.error("플랫폼 데이터 동기화 확인 실패", e);
        }
    }

    /**
     * 시스템 성능 메트릭을 주기적으로 수집합니다.
     * 1시간마다 실행되어 분석 시스템의 성능 지표를 수집합니다.
     */
    @Scheduled(fixedRate = 3600000) // 1시간 = 3,600,000ms
    public void collectPerformanceMetrics() {
        try {
            log.debug("분석 시스템 성능 메트릭 수집 시작");
            
            List<AnalysisExecutionJob> queuedJobs = analysisExecutionService.getQueuedJobs();
            List<AnalysisExecutionJob> processingJobs = analysisExecutionService.getProcessingJobs();
            List<AnalysisExecutionJob> retryableJobs = analysisExecutionService.getRetryableJobs();
            
            // 성능 메트릭 로깅
            log.info("=== 분석 시스템 성능 메트릭 ===");
            log.info("대기 중인 작업: {} 개", queuedJobs.size());
            log.info("처리 중인 작업: {} 개", processingJobs.size());
            log.info("재시도 대기 작업: {} 개", retryableJobs.size());
            
            // 메트릭 수집 서비스로 데이터 전송
            metricsService.recordAnalysisSystemMetrics(queuedJobs.size(), 
                processingJobs.size(), retryableJobs.size());
            
            log.debug("분석 시스템 성능 메트릭 수집 완료");
            
        } catch (Exception e) {
            log.error("성능 메트릭 수집 실패", e);
        }
    }
} 