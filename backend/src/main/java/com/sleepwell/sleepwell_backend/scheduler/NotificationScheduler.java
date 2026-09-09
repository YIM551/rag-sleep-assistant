package com.sleepwell.sleepwell_backend.scheduler;

import com.sleepwell.sleepwell_backend.service.NotificationService;
import com.sleepwell.sleepwell_backend.service.MetricsService;
import com.sleepwell.sleepwell_backend.service.PersonalizedSchedulerService;
import com.sleepwell.sleepwell_backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 알림 스케줄링 시스템
 * 
 * 예약된 알림들을 주기적으로 처리하고 
 * 만료된 알림들의 정리를 관리하는 스케줄러입니다.
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
    value = "sleepwell.scheduler.notification.enabled", 
    havingValue = "true", 
    matchIfMissing = true
)
public class NotificationScheduler {

    private final NotificationService notificationService;
    private final MetricsService metricsService;
    private final PersonalizedSchedulerService personalizedSchedulerService;
    private final NotificationRepository notificationRepository;

    /**
     * 발송 대기 중인 예약 알림들을 주기적으로 처리합니다.
     * 1분마다 실행되어 발송 시간이 된 알림들을 처리합니다.
     */
    @Scheduled(fixedRate = 60000) // 1분 = 60,000ms
    public void processPendingNotifications() {
        try {
            log.debug("예약 알림 처리 시작");
            notificationService.processPendingNotifications();
            log.debug("예약 알림 처리 완료");
            
        } catch (Exception e) {
            log.error("예약 알림 처리 실패", e);
        }
    }

    /**
     * 푸시 알림 발송 실패 시 재시도를 주기적으로 수행합니다.
     * 5분마다 실행되어 재시도 가능한 푸시 알림들을 처리합니다.
     */
    @Scheduled(fixedRate = 300000) // 5분 = 300,000ms
    public void retryFailedPushNotifications() {
        try {
            log.debug("푸시 알림 재시도 시작");
            notificationService.retryFailedPushNotifications();
            log.debug("푸시 알림 재시도 완료");
            
        } catch (Exception e) {
            log.error("푸시 알림 재시도 실패", e);
        }
    }

    /**
     * 만료된 알림들의 정리를 주기적으로 수행합니다.
     * 매일 새벽 3시에 실행되어 오래된 만료 알림들을 정리합니다.
     */
    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시
    public void cleanupExpiredNotifications() {
        try {
            log.info("만료된 알림 정리 시작");
            notificationService.cleanupExpiredNotifications();
            log.info("만료된 알림 정리 완료");
            
        } catch (Exception e) {
            log.error("만료된 알림 정리 실패", e);
        }
    }

    /**
     * 알림 시스템 성능 메트릭을 주기적으로 수집합니다.
     * 30분마다 실행되어 알림 시스템의 성능 지표를 수집합니다.
     */
    @Scheduled(fixedRate = 1800000) // 30분 = 1,800,000ms
    public void collectNotificationMetrics() {
        try {
            log.debug("알림 시스템 성능 메트릭 수집 시작");
            
            // 오늘 날짜 기준으로 메트릭 수집
            LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            LocalDateTime now = LocalDateTime.now();
            
            // 발송 대기 중인 알림 개수
            int pendingCount = notificationRepository.countPendingNotifications();
            
            // 오늘 발송된 알림 통계
            int sentTodayCount = notificationRepository.countByCreatedAtBetweenAndIsSentTrue(todayStart, now);
            int failedTodayCount = notificationRepository.countByCreatedAtBetweenAndIsSentFalse(todayStart, now);
            
            // 메트릭 서비스로 데이터 전송
            metricsService.recordNotificationMetrics(pendingCount, sentTodayCount, failedTodayCount);
            
            log.info("알림 시스템 메트릭 - 대기: {}, 오늘 발송: {}, 오늘 실패: {}", 
                pendingCount, sentTodayCount, failedTodayCount);
            
            log.debug("알림 시스템 성능 메트릭 수집 완료");
            
        } catch (Exception e) {
            log.error("알림 시스템 성능 메트릭 수집 실패", e);
        }
    }

    /**
     * 개인화된 수면 알림 스케줄을 초기화합니다.
     * 매일 자정에 실행되어 사용자별 맞춤 알림 스케줄을 재설정합니다.
     * 
     * 기존의 하드코딩된 오후 10시 스케줄을 대체하여 
     * 사용자별 설정에 따른 동적 스케줄링을 수행합니다.
     */
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정 (스케줄 재설정)
    public void initializeDailyPersonalizedSchedules() {
        try {
            log.info("개인화된 일일 알림 스케줄 초기화 시작");
            
            // 모든 사용자의 개인화된 수면 알림 스케줄링
            personalizedSchedulerService.scheduleAllPersonalizedSleepReminders();
            
            // 모든 사용자의 개인화된 기상 알림 스케줄링
            personalizedSchedulerService.scheduleAllPersonalizedWakeUpReminders();
            
            log.info("개인화된 일일 알림 스케줄 초기화 완료");
            
        } catch (Exception e) {
            log.error("개인화된 일일 알림 스케줄 초기화 실패", e);
        }
    }

    /**
     * 구독 관련 알림을 주기적으로 확인하고 생성합니다.
     * 매일 오전 10시에 실행되어 구독 만료 임박 알림을 생성합니다.
     */
    @Scheduled(cron = "0 0 10 * * *") // 매일 오전 10시
    public void scheduleSubscriptionNotifications() {
        try {
            log.info("구독 관련 알림 스케줄링 시작");
            
            // 구독 상태에 따른 알림 생성
            notificationService.scheduleSubscriptionAlerts();
            
            log.info("구독 관련 알림 스케줄링 완료");
            
        } catch (Exception e) {
            log.error("구독 관련 알림 스케줄링 실패", e);
        }
    }

    /**
     * 개인화된 수면 팁 알림을 주기적으로 생성합니다.
     * 매일 저녁 7시에 실행되어 사용자별 맞춤 수면 팁을 발송합니다.
     * 저녁 시간에 발송하여 "오늘밤부터 적용해봐야지" 하는 동기를 유발합니다.
     */
    @Scheduled(cron = "0 0 19 * * *") // 매일 저녁 7시
    public void schedulePersonalizedSleepTips() {
        try {
            log.info("개인화된 수면 팁 스케줄링 시작");
            
            // 사용자별 수면 패턴에 따른 개인화된 팁 생성
            notificationService.schedulePersonalizedSleepTips();
            
            log.info("개인화된 수면 팁 스케줄링 완료");
            
        } catch (Exception e) {
            log.error("개인화된 수면 팁 스케줄링 실패", e);
        }
    }

} 