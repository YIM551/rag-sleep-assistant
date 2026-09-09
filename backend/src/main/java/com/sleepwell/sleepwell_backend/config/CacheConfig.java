package com.sleepwell.sleepwell_backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * 통합 캐시 설정 (Caffeine Cache 기반)
 *
 * Caffeine을 사용하여 고성능 메모리 캐싱을 제공합니다.
 * - 최대 5,000개 항목 저장 (약 53MB, JVM Heap 2.8GB의 1.9%)
 * - 7일 후 자동 만료 (반복 쿼리 최적화)
 * - 통계 수집 지원
 * - LRU 정책으로 메모리 안전 보장
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Caffeine 기반 캐시 매니저
     * - LRU 방식으로 오래된 항목 자동 제거
     * - TTL로 신선도 보장
     * - 메모리 안정성 확보 (4GB RAM, JVM Heap 2.8GB 환경)
     * - 성능 최적화: 5,000개 항목, 7일 TTL (GPT-5 캐시 90% 할인 활용)
     *
     * @return CacheManager 인스턴스
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            // 기존 캐시
            "userProfile",

            // ASMR 관련 캐시
            "asmr-streaming",
            "asmr-preview",
            "asmr-content",
            "asmr-quality",
            "asmr-urls",

            // RAG 관련 캐시
            "rag-query-expansion",  // 쿼리 확장 캐시 (LLM 리라이트 결과)
            "rag-prf-seeds"         // PRF 시드 문서 캐시
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(5000)  // 1,000 → 5,000 (약 53MB, GPT-5 캐시 히트율 최적화)
                .expireAfterWrite(7, TimeUnit.DAYS)  // 24h → 7일 (반복 쿼리 캐싱)
                .recordStats());  // 통계 수집 (Actuator로 모니터링 가능)

        return cacheManager;
    }
} 