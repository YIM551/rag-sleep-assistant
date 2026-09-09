package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.dto.notification.NotificationDto;
import com.sleepwell.sleepwell_backend.enums.NotificationType;
import com.sleepwell.sleepwell_backend.enums.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Map;

public interface NotificationService {

    /**
     * 사용자에게 알림을 생성하고 푸시 알림을 발송합니다.
     *
     * @param userId 사용자 ID
     * @param notificationType 알림 유형
     * @param title 알림 제목
     * @param body 알림 내용
     * @return 생성된 알림 DTO
     */
    NotificationDto createAndSendNotification(Long userId, NotificationType notificationType, String title, String body);

    /**
     * 예약된 알림을 생성합니다.
     *
     * @param userId 사용자 ID
     * @param notificationType 알림 유형
     * @param title 알림 제목
     * @param body 알림 내용
     * @param scheduledAt 발송 예약 시간
     * @param expiresAt 만료 시간 (선택적)
     * @param priority 우선순위 (선택적)
     * @return 생성된 알림 DTO
     */
    NotificationDto scheduleNotification(Long userId, NotificationType notificationType, String title, String body,
                                        LocalDateTime scheduledAt, LocalDateTime expiresAt, Priority priority);

    /**
     * 관련 데이터와 연결된 알림을 생성합니다.
     *
     * @param userId 사용자 ID
     * @param notificationType 알림 유형
     * @param title 알림 제목
     * @param body 알림 내용
     * @param relatedDataId 관련 데이터 ID
     * @param relatedDataType 관련 데이터 타입
     * @param priority 우선순위 (선택적)
     * @return 생성된 알림 DTO
     */
    NotificationDto createRelatedNotification(Long userId, NotificationType notificationType, String title, String body,
                                             Long relatedDataId, String relatedDataType, Priority priority);

    // === 템플릿 기반 알림 생성 메서드들 ===

    /**
     * 템플릿을 사용하여 즉시 알림을 생성하고 발송합니다.
     *
     * @param userId 사용자 ID
     * @param templateId 사용할 템플릿 ID
     * @param variables 템플릿 변수 맵
     * @return 생성된 알림 DTO
     */
    NotificationDto createAndSendNotificationWithTemplate(Long userId, Long templateId, Map<String, Object> variables);

    /**
     * 템플릿 이름을 사용하여 즉시 알림을 생성하고 발송합니다.
     *
     * @param userId 사용자 ID
     * @param templateName 사용할 템플릿 이름
     * @param variables 템플릿 변수 맵
     * @return 생성된 알림 DTO
     */
    NotificationDto createAndSendNotificationWithTemplateName(Long userId, String templateName, Map<String, Object> variables);

    /**
     * 최적의 템플릿을 자동 선택하여 즉시 알림을 생성하고 발송합니다.
     *
     * @param userId 사용자 ID
     * @param notificationType 알림 타입
     * @param locale 언어/로케일
     * @param variables 템플릿 변수 맵
     * @return 생성된 알림 DTO
     */
    NotificationDto createAndSendNotificationWithAutoTemplate(Long userId, NotificationType notificationType, 
                                                              String locale, Map<String, Object> variables);

    /**
     * 템플릿을 사용하여 예약 알림을 생성합니다.
     *
     * @param userId 사용자 ID
     * @param templateId 사용할 템플릿 ID
     * @param variables 템플릿 변수 맵
     * @param scheduledAt 발송 예약 시간
     * @return 생성된 알림 DTO
     */
    NotificationDto scheduleNotificationWithTemplate(Long userId, Long templateId, Map<String, Object> variables,
                                                    LocalDateTime scheduledAt);

    /**
     * 템플릿을 사용하여 관련 데이터 알림을 생성합니다.
     *
     * @param userId 사용자 ID
     * @param templateId 사용할 템플릿 ID
     * @param variables 템플릿 변수 맵
     * @param relatedDataId 관련 데이터 ID
     * @param relatedDataType 관련 데이터 타입
     * @return 생성된 알림 DTO
     */
    NotificationDto createRelatedNotificationWithTemplate(Long userId, Long templateId, Map<String, Object> variables,
                                                         Long relatedDataId, String relatedDataType);
    
    /**
     * 특정 사용자의 알림 목록을 조회합니다.
     *
     * @param userId   사용자 ID
     * @param pageable 페이징 정보
     * @return 페이징된 알림 DTO 목록
     */
    Page<NotificationDto> getUserNotifications(Long userId, Pageable pageable);

    /**
     * 특정 알림을 읽음 처리합니다.
     *
     * @param userId         사용자 ID
     * @param notificationId 알림 ID
     * @return 읽음 처리된 알림 DTO
     */
    NotificationDto markNotificationAsRead(Long userId, Long notificationId);

    /**
     * 특정 사용자의 모든 알림을 읽음 처리합니다.
     *
     * @param userId 사용자 ID
     */
    void markAllNotificationsAsRead(Long userId);

    /**
     * 특정 알림을 삭제합니다.
     *
     * @param userId         사용자 ID
     * @param notificationId 알림 ID
     */
    void deleteNotification(Long userId, Long notificationId);

    /**
     * 특정 사용자의 읽지 않은 알림 개수를 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 읽지 않은 알림 개수
     */
    long getUnreadNotificationCount(Long userId);

    // === 스케줄링 관련 메서드들 ===

    /**
     * 발송 대기 중인 알림들을 처리합니다.
     * 스케줄러에서 주기적으로 호출됩니다.
     */
    void processPendingNotifications();

    /**
     * 만료된 알림들을 정리합니다.
     * 스케줄러에서 주기적으로 호출됩니다.
     */
    void cleanupExpiredNotifications();

    /**
     * 푸시 알림 발송 실패 시 재시도를 처리합니다.
     * 스케줄러에서 주기적으로 호출됩니다.
     */
    void retryFailedPushNotifications();
    
    // === 개인화된 수면 알림 메서드들 ===
    
    /**
     * 사용자별 개인화된 수면 알림을 스케줄링합니다.
     * 사용자의 수면 패턴과 설정에 따라 최적의 알림 시간을 계산합니다.
     */
    void schedulePersonalizedSleepReminders();
    
    /**
     * 특정 사용자의 개인화된 수면 알림을 생성합니다.
     * 
     * @param userId 사용자 ID
     */
    void createPersonalizedSleepRemindersForUser(Long userId);
    
    /**
     * 구독 상태에 따른 알림을 스케줄링합니다.
     * 구독 만료 임박, 결제 실패 등의 알림을 생성합니다.
     */
    void scheduleSubscriptionAlerts();
    
    /**
     * 개인화된 수면 팁 알림을 스케줄링합니다.
     * 사용자별 수면 패턴을 분석하여 맞춤 팁을 제공합니다.
     */
    void schedulePersonalizedSleepTips();
} 