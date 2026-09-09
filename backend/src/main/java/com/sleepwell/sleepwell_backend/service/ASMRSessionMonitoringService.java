package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.repository.ASMRPlaySessionRepository;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ASMR 세션 실시간 모니터링 서비스
 *
 * 활성 세션 수, 메모리 사용량, 성능 메트릭스를 실시간으로 추적하고
 * 임계치 초과 시 알림을 발생시킵니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ASMRSessionMonitoringService implements HealthIndicator {

    private final ASMRPlaySessionRepository sessionRepository;
    private final MeterRegistry meterRegistry;

    // 모니터링 메트릭스
    private AtomicLong currentActiveSessions = new AtomicLong(0);
    private AtomicLong totalSessionsCreated = new AtomicLong(0);
    private AtomicLong totalSessionsExpired = new AtomicLong(0);
    private AtomicLong rateLimitViolations = new AtomicLong(0);

    // 알림 임계치 설정
    private static final long MAX_ACTIVE_SESSIONS = 1000L;
    private static final long MAX_SESSIONS_PER_MINUTE = 100L;
    private static final double MAX_MEMORY_USAGE_PERCENT = 85.0;

    // 성능 메트릭스
    private Gauge activeSessionsGauge;
    private Counter sessionsCreatedCounter;
    private Counter sessionsExpiredCounter;
    private Counter rateLimitViolationsCounter;
    private Timer sessionCreationTimer;

    @PostConstruct
    public void initializeMetrics() {
        log.info("ASMR 세션 모니터링 서비스 초기화 시작");

        // Gauge 메트릭스 등록
        activeSessionsGauge = Gauge.builder("asmr.sessions.active", currentActiveSessions, AtomicLong::get)
                .description("현재 활성 ASMR 세션 수")
                .tag("service", "asmr")
                .register(meterRegistry);

        // Counter 메트릭스 등록
        sessionsCreatedCounter = Counter.builder("asmr.sessions.created.total")
                .description("생성된 ASMR 세션 총 수")
                .tag("service", "asmr")
                .register(meterRegistry);

        sessionsExpiredCounter = Counter.builder("asmr.sessions.expired.total")
                .description("만료된 ASMR 세션 총 수")
                .tag("service", "asmr")
                .register(meterRegistry);

        rateLimitViolationsCounter = Counter.builder("asmr.ratelimit.violations.total")
                .description("Rate Limiting 위반 총 수")
                .tag("service", "asmr")
                .register(meterRegistry);

        // Timer 메트릭스 등록
        sessionCreationTimer = Timer.builder("asmr.sessions.creation.duration")
                .description("ASMR 세션 생성 소요 시간")
                .tag("service", "asmr")
                .register(meterRegistry);

        log.info("ASMR 세션 모니터링 메트릭스 등록 완료");
    }

    /**
     * 세션 생성 이벤트 처리
     */
    public void onSessionCreated(Long userId, Long sessionId) {
        currentActiveSessions.incrementAndGet();
        totalSessionsCreated.incrementAndGet();
        sessionsCreatedCounter.increment();

        log.debug("세션 생성 감지: userId={}, sessionId={}, 현재 활성 세션={}",
                 userId, sessionId, currentActiveSessions.get());

        // 임계치 체크
        checkActiveSessionsThreshold();
    }

    /**
     * 세션 종료 이벤트 처리
     */
    public void onSessionEnded(Long userId, Long sessionId, String reason) {
        currentActiveSessions.decrementAndGet();
        if ("TIMER_EXPIRED".equals(reason)) {
            totalSessionsExpired.incrementAndGet();
            sessionsExpiredCounter.increment();
        }

        log.debug("세션 종료 감지: userId={}, sessionId={}, reason={}, 현재 활성 세션={}",
                 userId, sessionId, reason, currentActiveSessions.get());
    }

    /**
     * Rate Limiting 위반 이벤트 처리
     */
    public void onRateLimitViolation(String key, String limiterName) {
        rateLimitViolations.incrementAndGet();
        rateLimitViolationsCounter.increment();

        log.warn("Rate Limiting 위반: key={}, limiter={}, 총 위반 수={}",
                key, limiterName, rateLimitViolations.get());
    }

    /**
     * 세션 생성 시간 측정
     */
    public <T> T recordSessionCreationTime(Timer.Sample sample, T result) {
        sample.stop(sessionCreationTimer);
        return result;
    }

    /**
     * 실시간 세션 수 동기화 (5분마다 실행)
     */
    public void synchronizeActiveSessionCount() {
        try {
            long dbActiveCount = sessionRepository.countSessionsSince(LocalDateTime.now().minusHours(12));
            long currentCount = currentActiveSessions.get();

            // 차이가 10% 이상인 경우 동기화
            if (Math.abs(dbActiveCount - currentCount) > Math.max(1, currentCount * 0.1)) {
                currentActiveSessions.set(dbActiveCount);
                log.info("활성 세션 수 동기화: 기존={}, DB 실제={}", currentCount, dbActiveCount);
            }

        } catch (Exception e) {
            log.error("활성 세션 수 동기화 실패", e);
        }
    }

    /**
     * 메모리 사용량 모니터링 (1분마다 실행)
     */
    public void monitorMemoryUsage() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double usagePercent = (double) usedMemory / totalMemory * 100;

            // 메트릭스에 기록 (이미 등록된 경우 업데이트)
            meterRegistry.gauge("asmr.memory.usage.percent",
                    Tags.of("service", "asmr"), usagePercent);

            // 임계치 체크
            if (usagePercent > MAX_MEMORY_USAGE_PERCENT) {
                sendMemoryUsageAlert(usagePercent);
            }

        } catch (Exception e) {
            log.error("메모리 사용량 모니터링 실패", e);
        }
    }

    /**
     * 활성 세션 수 임계치 체크
     */
    private void checkActiveSessionsThreshold() {
        long activeCount = currentActiveSessions.get();

        if (activeCount > MAX_ACTIVE_SESSIONS) {
            sendActiveSessionsAlert(activeCount);
        }
    }

    /**
     * 활성 세션 수 초과 알림
     */
    private void sendActiveSessionsAlert(long activeCount) {
        log.error("🚨 활성 세션 수 임계치 초과! 현재: {}, 임계치: {}", activeCount, MAX_ACTIVE_SESSIONS);

        // 여기에 실제 알림 로직 추가 (예: Slack, 이메일, SMS)
        // alertService.sendAlert("ASMR 활성 세션 수 초과", "현재: " + activeCount);
    }

    /**
     * 메모리 사용량 초과 알림
     */
    private void sendMemoryUsageAlert(double usagePercent) {
        log.error("🚨 메모리 사용량 임계치 초과! 현재: {}%, 임계치: {}%",
                 String.format("%.1f", usagePercent), String.format("%.1f", MAX_MEMORY_USAGE_PERCENT));

        // 여기에 실제 알림 로직 추가
        // alertService.sendAlert("ASMR 메모리 사용량 초과", "현재: " + usagePercent + "%");
    }

    /**
     * Health Check 구현
     */
    @Override
    public Health health() {
        long activeCount = currentActiveSessions.get();
        long rateLimitViolationsCount = rateLimitViolations.get();

        Health.Builder builder = new Health.Builder();

        // 상태 판단
        if (activeCount > MAX_ACTIVE_SESSIONS * 0.9) {
            builder = builder.down();
        } else if (activeCount > MAX_ACTIVE_SESSIONS * 0.7) {
            builder = builder.status("WARNING");
        } else {
            builder = builder.up();
        }

        return builder
                .withDetail("activeSessions", activeCount)
                .withDetail("maxActiveSessions", MAX_ACTIVE_SESSIONS)
                .withDetail("totalSessionsCreated", totalSessionsCreated.get())
                .withDetail("totalSessionsExpired", totalSessionsExpired.get())
                .withDetail("rateLimitViolations", rateLimitViolationsCount)
                .withDetail("lastUpdated", LocalDateTime.now())
                .build();
    }

    /**
     * 현재 통계 조회
     */
    public ASMRSessionStats getCurrentStats() {
        return ASMRSessionStats.builder()
                .activeSessions(currentActiveSessions.get())
                .totalSessionsCreated(totalSessionsCreated.get())
                .totalSessionsExpired(totalSessionsExpired.get())
                .rateLimitViolations(rateLimitViolations.get())
                .maxActiveSessions(MAX_ACTIVE_SESSIONS)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * ASMR 세션 통계 DTO
     */
    @lombok.Builder
    @lombok.Getter
    public static class ASMRSessionStats {
        private final long activeSessions;
        private final long totalSessionsCreated;
        private final long totalSessionsExpired;
        private final long rateLimitViolations;
        private final long maxActiveSessions;
        private final LocalDateTime timestamp;
    }
}