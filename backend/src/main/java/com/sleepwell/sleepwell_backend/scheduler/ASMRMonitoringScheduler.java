package com.sleepwell.sleepwell_backend.scheduler;

import com.sleepwell.sleepwell_backend.service.ASMRSessionMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * ASMR 모니터링 스케줄러
 * ASMRSessionMonitoringService의 주기적 작업을 담당
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ASMRMonitoringScheduler {

    private final ASMRSessionMonitoringService monitoringService;

    /**
     * 실시간 세션 수 동기화 (5분마다 실행)
     */
    @Scheduled(fixedDelay = 300000) // 5분
    @Async("taskExecutor")
    public void synchronizeActiveSessionCount() {
        monitoringService.synchronizeActiveSessionCount();
    }

    /**
     * 메모리 사용량 모니터링 (1분마다 실행)
     */
    @Scheduled(fixedDelay = 60000) // 1분
    @Async("taskExecutor")
    public void monitorMemoryUsage() {
        monitoringService.monitorMemoryUsage();
    }
}