package com.sleepwell.sleepwell_backend.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ASMR 메트릭 서비스
 * Micrometer를 이용한 스트리밍 성능 모니터링
 */
@Service
@Slf4j
public class ASMRMetricsService {

    private final MeterRegistry meterRegistry;

    // 카운터
    private final Counter uploadCounter;
    private final Counter streamingCounter;
    private final Counter playCompleteCounter;
    private final Counter errorCounter;

    // 타이머
    private final Timer uploadTimer;
    private final Timer streamingTimer;

    // 게이지용 데이터
    private final AtomicInteger activeStreams = new AtomicInteger(0);
    private final AtomicLong totalBandwidth = new AtomicLong(0);
    private final AtomicInteger cacheHits = new AtomicInteger(0);
    private final AtomicInteger cacheMisses = new AtomicInteger(0);

    public ASMRMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // 카운터 초기화
        this.uploadCounter = Counter.builder("asmr.uploads")
                .description("ASMR 파일 업로드 총 횟수")
                .register(meterRegistry);

        this.streamingCounter = Counter.builder("asmr.streams")
                .description("ASMR 스트리밍 총 횟수")
                .register(meterRegistry);

        this.playCompleteCounter = Counter.builder("asmr.play.completed")
                .description("ASMR 재생 완료 총 횟수")
                .register(meterRegistry);

        this.errorCounter = Counter.builder("asmr.errors")
                .description("ASMR 서비스 오류 총 횟수")
                .register(meterRegistry);

        // 타이머 초기화
        this.uploadTimer = Timer.builder("asmr.upload.duration")
                .description("ASMR 파일 업로드 처리 시간")
                .register(meterRegistry);

        this.streamingTimer = Timer.builder("asmr.stream.duration")
                .description("ASMR 스트리밍 처리 시간")
                .register(meterRegistry);

        // 게이지 등록
        Gauge.builder("asmr.streams.active", this, ASMRMetricsService::getActiveStreams)
                .description("현재 활성 스트리밍 수")
                .register(meterRegistry);

        Gauge.builder("asmr.bandwidth.total", this, ASMRMetricsService::getTotalBandwidth)
                .description("총 사용 대역폭 (KB)")
                .register(meterRegistry);

        Gauge.builder("asmr.cache.hit.rate", this, ASMRMetricsService::getCacheHitRate)
                .description("캐시 히트 비율")
                .register(meterRegistry);

        log.info("ASMR 메트릭 서비스 초기화 완료");
    }

    /**
     * 파일 업로드 기록
     */
    public void recordUpload(String category, boolean success, long duration) {
        uploadCounter.increment();

        if (success) {
            uploadTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
            log.debug("업로드 메트릭 기록: category={}, duration={}ms", category, duration);
        } else {
            errorCounter.increment();
        }
    }

    /**
     * 스트리밍 시작 기록
     */
    public void recordStreamingStart(Long contentId, String quality) {
        streamingCounter.increment();
        activeStreams.incrementAndGet();

        // 품질별 대역폭 추정 (KB/s)
        int estimatedBandwidth = switch (quality.toLowerCase()) {
            case "high" -> 40;  // 320kbps ≈ 40KB/s
            case "medium" -> 16; // 128kbps ≈ 16KB/s
            case "low" -> 8;     // 64kbps ≈ 8KB/s
            default -> 16;
        };

        totalBandwidth.addAndGet(estimatedBandwidth);

        log.debug("스트리밍 시작 메트릭: contentId={}, quality={}, activeStreams={}",
                 contentId, quality, activeStreams.get());
    }

    /**
     * 스트리밍 종료 기록
     */
    public void recordStreamingEnd(Long contentId, String quality, long duration) {
        activeStreams.decrementAndGet();
        streamingTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);

        // 대역폭 사용량 감소
        int estimatedBandwidth = switch (quality.toLowerCase()) {
            case "high" -> 40;
            case "medium" -> 16;
            case "low" -> 8;
            default -> 16;
        };

        totalBandwidth.addAndGet(-estimatedBandwidth);

        log.debug("스트리밍 종료 메트릭: contentId={}, quality={}, duration={}ms",
                 contentId, quality, duration);
    }

    /**
     * 재생 완료 기록
     */
    public void recordPlayComplete(Long contentId, boolean fullCompletion) {
        playCompleteCounter.increment();

        if (fullCompletion) {
            meterRegistry.counter("asmr.play.full_completion").increment();
        }

        log.debug("재생 완료 메트릭: contentId={}, fullCompletion={}", contentId, fullCompletion);
    }

    /**
     * 캐시 히트 기록
     */
    public void recordCacheHit(String cacheName, boolean hit) {
        if (hit) {
            cacheHits.incrementAndGet();
        } else {
            cacheMisses.incrementAndGet();
        }

        meterRegistry.counter("asmr.cache.access", "cache", cacheName, "result", hit ? "hit" : "miss")
                .increment();
    }

    /**
     * 오류 기록
     */
    public void recordError(String errorType, String operation) {
        errorCounter.increment();

        meterRegistry.counter("asmr.errors.detailed", "type", errorType, "operation", operation)
                .increment();

        log.warn("ASMR 오류 메트릭: type={}, operation={}", errorType, operation);
    }

    // 게이지용 getter 메서드들
    private double getActiveStreams() {
        return activeStreams.get();
    }

    private double getTotalBandwidth() {
        return totalBandwidth.get();
    }

    private double getCacheHitRate() {
        int hits = cacheHits.get();
        int misses = cacheMisses.get();
        int total = hits + misses;

        return total > 0 ? (double) hits / total : 0.0;
    }
}