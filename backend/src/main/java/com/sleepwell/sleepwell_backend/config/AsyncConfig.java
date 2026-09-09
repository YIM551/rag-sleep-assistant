package com.sleepwell.sleepwell_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리 및 스케줄링 설정
 * 
 * SleepWell 백엔드의 비동기 작업 처리를 위한 설정입니다.
 * 분석 실행 시스템의 백그라운드 작업과 스케줄링을 담당합니다.
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Value("${sleepwell.async.core-pool-size:5}")
    private int corePoolSize;

    @Value("${sleepwell.async.max-pool-size:20}")
    private int maxPoolSize;

    @Value("${sleepwell.async.queue-capacity:100}")
    private int queueCapacity;

    @Value("${sleepwell.async.thread-name-prefix:sleepwell-async-}")
    private String threadNamePrefix;

    /**
     * 분석 실행을 위한 비동기 태스크 실행기를 구성합니다.
     * 
     * @return 구성된 ThreadPoolTaskExecutor
     */
    @Bean(name = "analysisTaskExecutor")
    public Executor analysisTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        
        // 우아한 종료 설정
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        // 거부된 작업 처리 정책
        executor.setRejectedExecutionHandler((runnable, executor1) -> {
            log.warn("분석 작업이 거부되었습니다. 큐가 가득 참: 활성 스레드={}, 큐 크기={}", 
                    executor1.getActiveCount(), executor1.getQueue().size());
            
            // 현재 스레드에서 실행 (CallerRunsPolicy와 유사)
            if (!executor1.isShutdown()) {
                runnable.run();
            }
        });
        
        executor.initialize();
        
        log.info("분석 태스크 실행기 초기화 완료 - 코어: {}, 최대: {}, 큐: {}", 
                corePoolSize, maxPoolSize, queueCapacity);
        
        return executor;
    }

    /**
     * 일반적인 비동기 작업을 위한 기본 실행기를 구성합니다.
     * 
     * @return 구성된 ThreadPoolTaskExecutor
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(Math.max(2, corePoolSize / 2));
        executor.setMaxPoolSize(Math.max(5, maxPoolSize / 2));
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("sleepwell-general-");
        
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        
        executor.initialize();
        
        log.info("일반 태스크 실행기 초기화 완료");
        
        return executor;
    }
} 