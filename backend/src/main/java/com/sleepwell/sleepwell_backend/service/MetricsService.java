package com.sleepwell.sleepwell_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 메트릭 수집 서비스
 * 시스템 성능, 비즈니스 메트릭을 수집하고 모니터링합니다.
 */
@Slf4j
@Service
public class MetricsService {
    
    private final MeterRegistry meterRegistry;
    
    // 분석 시스템 메트릭을 위한 게이지 값 저장
    private final Map<String, AtomicInteger> gaugeValues = new ConcurrentHashMap<>();
    
    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // 게이지 초기화
        gaugeValues.put("analysis.jobs.queued", new AtomicInteger(0));
        gaugeValues.put("analysis.jobs.processing", new AtomicInteger(0));
        gaugeValues.put("analysis.jobs.retryable", new AtomicInteger(0));
        gaugeValues.put("notifications.pending", new AtomicInteger(0));
        gaugeValues.put("notifications.sent.today", new AtomicInteger(0));
        gaugeValues.put("notifications.failed.today", new AtomicInteger(0));
        
        // 게이지 등록
        Gauge.builder("analysis.jobs.queued", gaugeValues.get("analysis.jobs.queued"), AtomicInteger::get)
            .description("대기 중인 분석 작업 수")
            .register(meterRegistry);
            
        Gauge.builder("analysis.jobs.processing", gaugeValues.get("analysis.jobs.processing"), AtomicInteger::get)
            .description("처리 중인 분석 작업 수")
            .register(meterRegistry);
            
        Gauge.builder("analysis.jobs.retryable", gaugeValues.get("analysis.jobs.retryable"), AtomicInteger::get)
            .description("재시도 대기 중인 분석 작업 수")
            .register(meterRegistry);
            
        Gauge.builder("notifications.pending", gaugeValues.get("notifications.pending"), AtomicInteger::get)
            .description("발송 대기 중인 알림 수")
            .register(meterRegistry);
            
        Gauge.builder("notifications.sent.today", gaugeValues.get("notifications.sent.today"), AtomicInteger::get)
            .description("오늘 발송된 알림 수")
            .register(meterRegistry);
            
        Gauge.builder("notifications.failed.today", gaugeValues.get("notifications.failed.today"), AtomicInteger::get)
            .description("오늘 발송 실패한 알림 수")
            .register(meterRegistry);
    }
    
    /**
     * 분석 시스템 메트릭 기록
     */
    public void recordAnalysisSystemMetrics(int queuedJobs, int processingJobs, int retryableJobs) {
        gaugeValues.get("analysis.jobs.queued").set(queuedJobs);
        gaugeValues.get("analysis.jobs.processing").set(processingJobs);
        gaugeValues.get("analysis.jobs.retryable").set(retryableJobs);
        
        log.debug("분석 시스템 메트릭 업데이트 - 대기: {}, 처리중: {}, 재시도대기: {}", 
            queuedJobs, processingJobs, retryableJobs);
    }
    
    /**
     * 알림 시스템 메트릭 기록
     */
    public void recordNotificationMetrics(int pendingCount, int sentTodayCount, int failedTodayCount) {
        gaugeValues.get("notifications.pending").set(pendingCount);
        gaugeValues.get("notifications.sent.today").set(sentTodayCount);
        gaugeValues.get("notifications.failed.today").set(failedTodayCount);
        
        log.debug("알림 시스템 메트릭 업데이트 - 대기: {}, 오늘발송: {}, 오늘실패: {}", 
            pendingCount, sentTodayCount, failedTodayCount);
    }
    
    /**
     * 분석 작업 완료 카운터 증가
     */
    public void incrementAnalysisCompleted() {
        Counter.builder("analysis.jobs.completed")
            .description("완료된 분석 작업 수")
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * 분석 작업 실패 카운터 증가
     */
    public void incrementAnalysisFailed() {
        Counter.builder("analysis.jobs.failed")
            .description("실패한 분석 작업 수")
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * 알림 발송 성공 카운터 증가
     */
    public void incrementNotificationSent(String type) {
        Counter.builder("notifications.sent")
            .description("발송된 알림 수")
            .tag("type", type)
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * 알림 발송 실패 카운터 증가
     */
    public void incrementNotificationFailed(String type) {
        Counter.builder("notifications.failed")
            .description("발송 실패한 알림 수")
            .tag("type", type)
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * 플랫폼 동기화 성공 카운터 증가
     */
    public void incrementPlatformSyncSuccess(String platform) {
        Counter.builder("platform.sync.success")
            .description("플랫폼 동기화 성공 수")
            .tag("platform", platform)
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * 플랫폼 동기화 실패 카운터 증가
     */
    public void incrementPlatformSyncFailed(String platform) {
        Counter.builder("platform.sync.failed")
            .description("플랫폼 동기화 실패 수")
            .tag("platform", platform)
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * 결제 성공 카운터 증가
     */
    public void incrementPaymentSuccess(String method, long amount) {
        Counter.builder("payments.success")
            .description("성공한 결제 수")
            .tag("method", method)
            .register(meterRegistry)
            .increment();
            
        Counter.builder("payments.amount")
            .description("결제 금액")
            .tag("method", method)
            .register(meterRegistry)
            .increment(amount);
    }
    
    /**
     * 결제 실패 카운터 증가
     */
    public void incrementPaymentFailed(String method) {
        Counter.builder("payments.failed")
            .description("실패한 결제 수")
            .tag("method", method)
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * 수면 기록 생성 카운터 증가
     */
    public void incrementSleepRecordCreated(String source) {
        Counter.builder("sleep.records.created")
            .description("생성된 수면 기록 수")
            .tag("source", source)
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * 수면 품질 점수 평균 기록
     */
    public void recordSleepQualityScore(double score) {
        meterRegistry.summary("sleep.quality.score")
            .record(score);
    }
}