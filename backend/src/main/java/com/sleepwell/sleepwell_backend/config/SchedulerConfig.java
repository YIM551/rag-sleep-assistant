package com.sleepwell.sleepwell_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 스케줄러 설정
 * 
 * 개인화된 알림 스케줄링을 위한 TaskScheduler 설정을 제공합니다.
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Configuration
public class SchedulerConfig {

    /**
     * 개인화된 알림 스케줄링을 위한 TaskScheduler Bean
     * 
     * @return ThreadPoolTaskScheduler 인스턴스
     */
    @Bean(name = "taskScheduler")
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10); // 동시 실행 가능한 스케줄 수
        scheduler.setThreadNamePrefix("PersonalizedScheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        return scheduler;
    }
}