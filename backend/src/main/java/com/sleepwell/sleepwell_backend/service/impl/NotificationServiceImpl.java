package com.sleepwell.sleepwell_backend.service.impl;
import com.sleepwell.sleepwell_backend.dto.notification.NotificationDto;
import com.sleepwell.sleepwell_backend.entity.Notification;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.enums.NotificationType;
import com.sleepwell.sleepwell_backend.enums.Priority;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.repository.NotificationRepository;
import com.sleepwell.sleepwell_backend.repository.UserRepository;
import com.sleepwell.sleepwell_backend.service.NotificationService;
import com.sleepwell.sleepwell_backend.service.NotificationTemplateService;
import com.sleepwell.sleepwell_backend.service.FcmPushService;
import com.sleepwell.sleepwell_backend.repository.SubscriptionRepository;
import com.sleepwell.sleepwell_backend.entity.Subscription;
import com.sleepwell.sleepwell_backend.enums.SubscriptionStatus;
import com.sleepwell.sleepwell_backend.repository.SleepRecordRepository;
import com.sleepwell.sleepwell_backend.entity.SleepRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationTemplateService templateService;
    private final SubscriptionRepository subscriptionRepository;
    private final FcmPushService fcmPushService;
    
    // 원래 생성자 (기존 테스트와의 호환성 유지)
    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   UserRepository userRepository,
                                   NotificationTemplateService templateService,
                                   SubscriptionRepository subscriptionRepository,
                                   FcmPushService fcmPushService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.templateService = templateService;
        this.subscriptionRepository = subscriptionRepository;
        this.fcmPushService = fcmPushService;
    }
    
    // Spring이 자동으로 주입할 SleepRecordRepository (개인화된 수면 팁용)
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private SleepRecordRepository sleepRecordRepository;

    @Override
    @Transactional
    public NotificationDto createAndSendNotification(Long userId, NotificationType notificationType, String title, String body) {
        User user = findUserById(userId);

        Notification notification = createAndSaveNotification(user, notificationType, title, body, null, null, null, null, null);
        sendPushNotificationIfTokenExists(user, title, body);

        log.info("즉시 알림 생성 및 발송 완료: 사용자 ID {}, 타입 {}", userId, notificationType);
        return new NotificationDto(notification);
    }

    @Override
    @Transactional
    public NotificationDto scheduleNotification(Long userId, NotificationType notificationType, String title, String body,
                                               LocalDateTime scheduledAt, LocalDateTime expiresAt, Priority priority) {
        User user = findUserById(userId);

        Notification notification = createAndSaveNotification(user, notificationType, title, body, scheduledAt, expiresAt, priority, null, null);

        log.info("예약 알림 생성 완료: 사용자 ID {}, 타입 {}, 예약 시간 {}", userId, notificationType, scheduledAt);
        return new NotificationDto(notification);
    }

    @Override
    @Transactional
    public NotificationDto createRelatedNotification(Long userId, NotificationType notificationType, String title, String body,
                                                    Long relatedDataId, String relatedDataType, Priority priority) {
        User user = findUserById(userId);

        Notification notification = createAndSaveNotification(user, notificationType, title, body, null, null, priority, relatedDataId, relatedDataType);
        sendPushNotificationIfTokenExists(user, title, body);

        log.info("관련 데이터 알림 생성 완료: 사용자 ID {}, 타입 {}, 관련 데이터 {}:{}", userId, notificationType, relatedDataType, relatedDataId);
        return new NotificationDto(notification);
    }

    // === 템플릿 기반 알림 생성 메서드들 ===

    @Override
    @Transactional
    public NotificationDto createAndSendNotificationWithTemplate(Long userId, Long templateId, Map<String, Object> variables) {
        User user = findUserById(userId);

        // 템플릿을 사용하여 제목과 메시지 생성
        Map<String, String> content = templateService.generateNotificationContent(templateId, variables);
        String title = content.get("title");
        String message = content.get("message");

        // 템플릿 정보 조회하여 기본값 적용
        var template = templateService.getTemplate(templateId);
        
        Notification notification = createAndSaveNotification(
                user, 
                template.getType(), 
                title, 
                message, 
                null, 
                template.getDefaultExpiryHours() != null ? LocalDateTime.now().plusHours(template.getDefaultExpiryHours()) : null,
                template.getDefaultPriority(), 
                null, 
                null
        );
        
        sendPushNotificationIfTokenExists(user, title, message);

        log.info("템플릿 기반 즉시 알림 생성 완료: 사용자 ID {}, 템플릿 ID {}", userId, templateId);
        return new NotificationDto(notification);
    }

    @Override
    @Transactional
    public NotificationDto createAndSendNotificationWithTemplateName(Long userId, String templateName, Map<String, Object> variables) {
        User user = findUserById(userId);

        // 템플릿 이름으로 제목과 메시지 생성
        Map<String, String> content = templateService.generateNotificationContentByName(templateName, variables);
        String title = content.get("title");
        String message = content.get("message");

        // 템플릿 정보 조회하여 기본값 적용
        var template = templateService.getTemplateByName(templateName);
        
        Notification notification = createAndSaveNotification(
                user, 
                template.getType(), 
                title, 
                message, 
                null, 
                template.getDefaultExpiryHours() != null ? LocalDateTime.now().plusHours(template.getDefaultExpiryHours()) : null,
                template.getDefaultPriority(), 
                null, 
                null
        );
        
        sendPushNotificationIfTokenExists(user, title, message);

        log.info("템플릿 이름 기반 즉시 알림 생성 완료: 사용자 ID {}, 템플릿 이름 {}", userId, templateName);
        return new NotificationDto(notification);
    }

    @Override
    @Transactional
    public NotificationDto createAndSendNotificationWithAutoTemplate(Long userId, NotificationType notificationType, 
                                                                    String locale, Map<String, Object> variables) {
        User user = findUserById(userId);

        // 자동 템플릿 선택하여 제목과 메시지 생성
        Map<String, String> content = templateService.generateNotificationContentAuto(notificationType, locale, variables);
        String title = content.get("title");
        String message = content.get("message");

        Notification notification = createAndSaveNotification(
                user, 
                notificationType, 
                title, 
                message, 
                null, 
                null,
                Priority.NORMAL, 
                null, 
                null
        );
        
        sendPushNotificationIfTokenExists(user, title, message);

        log.info("자동 템플릿 기반 즉시 알림 생성 완료: 사용자 ID {}, 타입 {}, 로케일 {}", userId, notificationType, locale);
        return new NotificationDto(notification);
    }

    @Override
    @Transactional
    public NotificationDto scheduleNotificationWithTemplate(Long userId, Long templateId, Map<String, Object> variables,
                                                           LocalDateTime scheduledAt) {
        User user = findUserById(userId);

        // 템플릿을 사용하여 제목과 메시지 생성
        Map<String, String> content = templateService.generateNotificationContent(templateId, variables);
        String title = content.get("title");
        String message = content.get("message");

        // 템플릿 정보 조회하여 기본값 적용
        var template = templateService.getTemplate(templateId);
        
        Notification notification = createAndSaveNotification(
                user, 
                template.getType(), 
                title, 
                message, 
                scheduledAt, 
                template.getDefaultExpiryHours() != null ? scheduledAt.plusHours(template.getDefaultExpiryHours()) : null,
                template.getDefaultPriority(), 
                null, 
                null
        );

        log.info("템플릿 기반 예약 알림 생성 완료: 사용자 ID {}, 템플릿 ID {}, 예약 시간 {}", userId, templateId, scheduledAt);
        return new NotificationDto(notification);
    }

    @Override
    @Transactional
    public NotificationDto createRelatedNotificationWithTemplate(Long userId, Long templateId, Map<String, Object> variables,
                                                                Long relatedDataId, String relatedDataType) {
        User user = findUserById(userId);

        // 템플릿을 사용하여 제목과 메시지 생성
        Map<String, String> content = templateService.generateNotificationContent(templateId, variables);
        String title = content.get("title");
        String message = content.get("message");

        // 템플릿 정보 조회하여 기본값 적용
        var template = templateService.getTemplate(templateId);
        
        Notification notification = createAndSaveNotification(
                user, 
                template.getType(), 
                title, 
                message, 
                null, 
                template.getDefaultExpiryHours() != null ? LocalDateTime.now().plusHours(template.getDefaultExpiryHours()) : null,
                template.getDefaultPriority(), 
                relatedDataId, 
                relatedDataType
        );
        
        sendPushNotificationIfTokenExists(user, title, message);

        log.info("템플릿 기반 관련 데이터 알림 생성 완료: 사용자 ID {}, 템플릿 ID {}, 관련 데이터 {}:{}", userId, templateId, relatedDataType, relatedDataId);
        return new NotificationDto(notification);
    }

    @Override
    public Page<NotificationDto> getUserNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationDto::new);
    }

    @Override
    @Transactional
    public NotificationDto markNotificationAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException("알림을 찾을 수 없거나 권한이 없습니다.", HttpStatus.NOT_FOUND));

        notification.markAsRead();
        return new NotificationDto(notification);
    }

    @Override
    @Transactional
    public void markAllNotificationsAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository.findUnreadByUserId(userId);
        for (Notification notification : unreadNotifications) {
            notification.markAsRead();
        }
        log.info("{}번 사용자의 모든 알림을 읽음 처리했습니다. ({}개)", userId, unreadNotifications.size());
    }

    @Override
    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException("알림을 찾을 수 없거나 권한이 없습니다.", HttpStatus.NOT_FOUND));

        notificationRepository.delete(notification);
        log.info("알림을 삭제했습니다: ID {}, 사용자 ID {}", notificationId, userId);
    }

    @Override
    public long getUnreadNotificationCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    // === 스케줄링 관련 메서드들 ===

    @Override
    @Transactional
    public void processPendingNotifications() {
        LocalDateTime now = LocalDateTime.now();
        List<Notification> pendingNotifications = notificationRepository.findPendingSend(now);

        log.info("발송 대기 중인 알림 처리 시작: {} 건", pendingNotifications.size());

        for (Notification notification : pendingNotifications) {
            try {
                // 만료 확인
                if (notification.isExpired()) {
                    log.debug("만료된 알림 건너뜀: ID {}", notification.getId());
                    continue;
                }

                // 푸시 알림 발송
                User user = notification.getUser();
                sendPushNotificationIfTokenExists(user, notification.getTitle(), notification.getMessage());
                
                // 발송 완료 처리
                notification.markAsSent();
                
                log.debug("예약 알림 발송 완료: ID {}, 사용자 ID {}", notification.getId(), user.getId());
                
            } catch (Exception e) {
                log.error("예약 알림 발송 실패: ID {}, 오류: {}", notification.getId(), e.getMessage(), e);
            }
        }

        log.info("발송 대기 중인 알림 처리 완료: {} 건 처리", pendingNotifications.size());
    }

    @Override
    @Transactional
    public void cleanupExpiredNotifications() {
        LocalDateTime now = LocalDateTime.now();
        List<Notification> expiredNotifications = notificationRepository.findExpired(now);

        log.info("만료된 알림 정리 시작: {} 건", expiredNotifications.size());

        for (Notification notification : expiredNotifications) {
            try {
                // 중요한 알림은 7일 추가 보관
                if (notification.getPriority() == Priority.URGENT || notification.getPriority() == Priority.HIGH) {
                    LocalDateTime extendedExpiry = notification.getExpiresAt().plusDays(7);
                    if (now.isBefore(extendedExpiry)) {
                        log.debug("중요 알림 보관 기간 연장: ID {}", notification.getId());
                        continue;
                    }
                }

                notificationRepository.delete(notification);
                log.debug("만료된 알림 삭제: ID {}", notification.getId());
                
            } catch (Exception e) {
                log.error("만료된 알림 삭제 실패: ID {}, 오류: {}", notification.getId(), e.getMessage(), e);
            }
        }

        log.info("만료된 알림 정리 완료: {} 건 처리", expiredNotifications.size());
    }

    @Override
    @Transactional
    public void retryFailedPushNotifications() {
        List<Notification> failedPushNotifications = notificationRepository.findPendingPush();

        log.info("푸시 알림 재시도 시작: {} 건", failedPushNotifications.size());

        for (Notification notification : failedPushNotifications) {
            try {
                // 만료 확인
                if (notification.isExpired()) {
                    log.debug("만료된 알림 재시도 건너뜀: ID {}", notification.getId());
                    continue;
                }

                // 푸시 알림 재시도
                User user = notification.getUser();
                sendPushNotificationIfTokenExists(user, notification.getTitle(), notification.getMessage());
                
                // 푸시 발송 완료 처리
                notification.markAsPushSent();
                
                log.debug("푸시 알림 재시도 성공: ID {}, 사용자 ID {}", notification.getId(), user.getId());
                
            } catch (Exception e) {
                log.error("푸시 알림 재시도 실패: ID {}, 오류: {}", notification.getId(), e.getMessage(), e);
            }
        }

        log.info("푸시 알림 재시도 완료: {} 건 처리", failedPushNotifications.size());
    }

    @Override
    @Transactional
    public void schedulePersonalizedSleepReminders() {
        log.info("개인화된 수면 알림 스케줄링 시작");
        
        // 알림을 받기로 설정한 모든 활성 사용자 조회
        List<User> activeUsers = userRepository.findAllByActiveTrue();
        
        int processedCount = 0;
        for (User user : activeUsers) {
            try {
                // 사용자별 알림 설정 확인
                if (Boolean.TRUE.equals(user.getNotificationEnabled())) {
                    createPersonalizedSleepRemindersForUser(user.getId());
                    processedCount++;
                }
            } catch (Exception e) {
                log.error("사용자 {} 수면 알림 생성 실패: {}", user.getId(), e.getMessage(), e);
            }
        }
        
        log.info("개인화된 수면 알림 스케줄링 완료: {} 명 처리", processedCount);
    }
    
    @Override
    @Transactional
    public void createPersonalizedSleepRemindersForUser(Long userId) {
        User user = findUserById(userId);
        
        // 사용자의 평균 수면 시간 조회
        LocalDateTime now = LocalDateTime.now();
        
        // 수면 준비 알림 (기본값: 오후 10시, 사용자 설정값이 있으면 그 값 사용)
        LocalDateTime bedtimeReminder = now.toLocalDate().atTime(22, 0); // 기본값
        if (user.getPreferredBedtime() != null) {
            bedtimeReminder = now.toLocalDate().atTime(user.getPreferredBedtime());
        }
        
        // 1시간 전 알림
        LocalDateTime reminderTime = bedtimeReminder.minusHours(1);
        
        // 오늘 이미 지난 시간이면 내일로 설정
        if (reminderTime.isBefore(now)) {
            reminderTime = reminderTime.plusDays(1);
        }
        
        // 수면 준비 알림 생성
        Notification bedtimeNotification = createAndSaveNotification(
            user,
            NotificationType.SLEEP_REMINDER,
            "수면 시간이 다가오고 있어요 🌙",
            "1시간 후면 수면 시간입니다. 편안한 수면을 위해 미리 준비해보세요.",
            reminderTime,
            reminderTime.plusHours(2), // 2시간 후 만료
            Priority.NORMAL,
            null,
            null
        );
        
        log.debug("수면 준비 알림 생성: 사용자 {}, 예약 시간 {}", userId, reminderTime);
        
        // 기상 후 수면 기록 알림 (기본값: 오전 8시)
        LocalDateTime wakeupTime = now.toLocalDate().atTime(8, 0); // 기본값
        if (user.getPreferredWakeupTime() != null) {
            wakeupTime = now.toLocalDate().atTime(user.getPreferredWakeupTime());
        }
        
        LocalDateTime recordReminder = wakeupTime.plusHours(1); // 기상 1시간 후
        
        // 오늘 이미 지난 시간이면 내일로 설정
        if (recordReminder.isBefore(now)) {
            recordReminder = recordReminder.plusDays(1);
        }
        
        // 수면 기록 알림 생성
        Notification recordNotification = createAndSaveNotification(
            user,
            NotificationType.SLEEP_RECORD_REMINDER,
            "어제 밤 수면은 어떠셨나요? 📊",
            "수면 기록을 남겨주시면 더 나은 수면 분석을 제공해드릴 수 있어요.",
            recordReminder,
            recordReminder.plusHours(4), // 4시간 후 만료
            Priority.LOW,
            null,
            null
        );
        
        log.debug("수면 기록 알림 생성: 사용자 {}, 예약 시간 {}", userId, recordReminder);
    }
    
    @Override
    @Transactional
    public void scheduleSubscriptionAlerts() {
        log.info("구독 관련 알림 스케줄링 시작");
        
        LocalDateTime now = LocalDateTime.now();
        
        // 1. 구독 만료 7일 전 알림
        LocalDateTime sevenDaysLater = now.plusDays(7);
        List<Subscription> expiringIn7Days = subscriptionRepository.findExpiringSubscriptions(
            now, sevenDaysLater, SubscriptionStatus.ACTIVE
        );
        
        for (Subscription subscription : expiringIn7Days) {
            try {
                // 이미 7일 전 알림을 보냈는지 확인
                LocalDateTime checkDate = subscription.getEndDate().minusDays(7).withHour(0).withMinute(0);
                boolean alreadySent = notificationRepository.existsByUserAndTypeAndScheduledAtBetween(
                    subscription.getUser(),
                    NotificationType.SUBSCRIPTION_EXPIRING_SOON,
                    checkDate,
                    checkDate.plusDays(1)
                );
                
                if (!alreadySent) {
                    createAndSaveNotification(
                        subscription.getUser(),
                        NotificationType.SUBSCRIPTION_EXPIRING_SOON,
                        "구독 만료 예정 알림 📅",
                        String.format("%s 구독이 7일 후 만료됩니다. 계속 서비스를 이용하시려면 구독을 갱신해주세요.", 
                            subscription.getPlanName() != null ? subscription.getPlanName() : "프리미엄"),
                        now,
                        subscription.getEndDate(),
                        Priority.HIGH,
                        subscription.getId(),
                        "SUBSCRIPTION"
                    );
                    log.debug("7일 전 구독 만료 알림 생성: 사용자 {}, 구독 {}", 
                        subscription.getUser().getId(), subscription.getId());
                }
            } catch (Exception e) {
                log.error("구독 만료 7일 전 알림 생성 실패: 구독 {}", subscription.getId(), e);
            }
        }
        
        // 2. 구독 만료 1일 전 알림
        LocalDateTime oneDayLater = now.plusDays(1);
        List<Subscription> expiringIn1Day = subscriptionRepository.findExpiringSubscriptions(
            now, oneDayLater, SubscriptionStatus.ACTIVE
        );
        
        for (Subscription subscription : expiringIn1Day) {
            try {
                // 이미 1일 전 알림을 보냈는지 확인
                LocalDateTime checkDate = subscription.getEndDate().minusDays(1).withHour(0).withMinute(0);
                boolean alreadySent = notificationRepository.existsByUserAndTypeAndScheduledAtBetween(
                    subscription.getUser(),
                    NotificationType.SUBSCRIPTION_EXPIRING_SOON,
                    checkDate,
                    checkDate.plusDays(1)
                );
                
                if (!alreadySent) {
                    createAndSaveNotification(
                        subscription.getUser(),
                        NotificationType.SUBSCRIPTION_EXPIRING_SOON,
                        "구독 만료 임박! ⚠️",
                        "내일 구독이 만료됩니다. 서비스 중단을 원하지 않으시면 지금 갱신해주세요.",
                        now,
                        subscription.getEndDate().plusHours(2),
                        Priority.URGENT,
                        subscription.getId(),
                        "SUBSCRIPTION"
                    );
                    log.debug("1일 전 구독 만료 알림 생성: 사용자 {}, 구독 {}", 
                        subscription.getUser().getId(), subscription.getId());
                }
            } catch (Exception e) {
                log.error("구독 만료 1일 전 알림 생성 실패: 구독 {}", subscription.getId(), e);
            }
        }
        
        // 3. 구독 만료 당일 알림
        LocalDateTime todayStart = now.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime todayEnd = todayStart.plusDays(1);
        List<Subscription> expiringToday = subscriptionRepository.findExpiringSubscriptions(
            todayStart, todayEnd, SubscriptionStatus.ACTIVE
        );
        
        for (Subscription subscription : expiringToday) {
            try {
                // 오늘 이미 알림을 보냈는지 확인
                boolean alreadySent = notificationRepository.existsByUserAndTypeAndScheduledAtBetween(
                    subscription.getUser(),
                    NotificationType.SUBSCRIPTION_EXPIRED,
                    todayStart,
                    todayEnd
                );
                
                if (!alreadySent) {
                    createAndSaveNotification(
                        subscription.getUser(),
                        NotificationType.SUBSCRIPTION_EXPIRED,
                        "구독이 만료되었습니다 😢",
                        "구독이 만료되어 일부 기능이 제한됩니다. 계속 이용하시려면 구독을 갱신해주세요.",
                        now,
                        now.plusDays(3), // 3일 후 만료
                        Priority.URGENT,
                        subscription.getId(),
                        "SUBSCRIPTION"
                    );
                    log.debug("구독 만료 당일 알림 생성: 사용자 {}, 구독 {}", 
                        subscription.getUser().getId(), subscription.getId());
                }
            } catch (Exception e) {
                log.error("구독 만료 당일 알림 생성 실패: 구독 {}", subscription.getId(), e);
            }
        }
        
        // 4. 자동 갱신이 꺼진 구독에 대한 알림
        LocalDateTime threeDaysLater = now.plusDays(3);
        List<Subscription> nonRenewingSubscriptions = subscriptionRepository.findNonRenewingExpired(
            threeDaysLater, SubscriptionStatus.ACTIVE
        );
        
        for (Subscription subscription : nonRenewingSubscriptions) {
            try {
                // 자동 갱신 비활성화 알림을 이미 보냈는지 확인
                LocalDateTime checkDate = subscription.getEndDate().minusDays(3).withHour(0).withMinute(0);
                boolean alreadySent = notificationRepository.existsByUserAndTypeAndScheduledAtBetween(
                    subscription.getUser(),
                    NotificationType.SUBSCRIPTION_EXPIRING_SOON,
                    checkDate,
                    checkDate.plusDays(1)
                );
                
                if (!alreadySent) {
                    createAndSaveNotification(
                        subscription.getUser(),
                        NotificationType.SUBSCRIPTION_EXPIRING_SOON,
                        "자동 갱신이 꺼져있습니다 🔄",
                        "구독이 3일 후 만료되며, 자동 갱신이 비활성화되어 있습니다. 계속 이용하시려면 수동으로 갱신해주세요.",
                        now,
                        subscription.getEndDate(),
                        Priority.NORMAL,
                        subscription.getId(),
                        "SUBSCRIPTION"
                    );
                    log.debug("자동 갱신 비활성화 알림 생성: 사용자 {}, 구독 {}", 
                        subscription.getUser().getId(), subscription.getId());
                }
            } catch (Exception e) {
                log.error("자동 갱신 비활성화 알림 생성 실패: 구독 {}", subscription.getId(), e);
            }
        }
        
        log.info("구독 관련 알림 스케줄링 완료");
    }

    @Override
    @Transactional
    public void schedulePersonalizedSleepTips() {
        if (sleepRecordRepository == null) {
            log.warn("SleepRecordRepository가 주입되지 않아 개인화된 수면 팁 스케줄링을 건너뜁니다");
            return;
        }
        
        log.info("개인화된 수면 팁 스케줄링 시작");
        
        // 활성 사용자 목록 조회 (최근 7일 내 수면 기록이 있는 사용자)
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<User> activeUsers = userRepository.findUsersWithRecentSleepRecords(weekAgo);
        
        log.info("개인화된 수면 팁 대상 사용자 {}명", activeUsers.size());
        
        int successCount = 0;
        int skipCount = 0;
        int errorCount = 0;
        
        for (User user : activeUsers) {
            try {
                boolean created = createPersonalizedSleepTipForUser(user);
                if (created) {
                    successCount++;
                } else {
                    skipCount++;
                }
            } catch (Exception e) {
                errorCount++;
                log.error("사용자 {}의 개인화된 수면 팁 생성 실패", user.getId(), e);
            }
        }
        
        log.info("개인화된 수면 팁 스케줄링 완료: 성공 {}건, 건너뜀 {}건, 오류 {}건", 
                successCount, skipCount, errorCount);
    }

    private boolean createPersonalizedSleepTipForUser(User user) {
        log.debug("사용자 {}의 개인화된 수면 팁 생성 시작", user.getId());
        
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime tomorrow = today.plusDays(1);
        
        // 오늘 이미 개인화된 수면 팁 알림을 받았는지 확인
        boolean alreadySent = notificationRepository.existsByUserAndTypeAndScheduledAtBetween(
                user,
                NotificationType.PERSONALIZED_SLEEP_TIP,
                today,
                tomorrow
        );
        
        if (alreadySent) {
            log.debug("사용자 {}는 오늘 이미 개인화된 수면 팁을 받음", user.getId());
            return false;
        }
        
        // 최근 7일 수면 기록 분석
        if (sleepRecordRepository == null) {
            log.warn("SleepRecordRepository가 주입되지 않아 사용자 {}의 수면 기록 조회를 건너뜀", user.getId());
            return false;
        }
        
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<SleepRecord> recentRecords = sleepRecordRepository.findByUserAndSleepStartTimeBetweenOrderBySleepStartTimeDesc(
                user, weekAgo, LocalDateTime.now()
        );
        
        if (recentRecords.isEmpty()) {
            log.debug("사용자 {}의 최근 수면 기록이 없어 팁 생성 건너뜀", user.getId());
            return false;
        }
        
        // 수면 패턴 분석
        SleepPatternAnalysis analysis = analyzeSleepPattern(user.getId(), recentRecords);
        
        // 개인화된 팁 생성
        PersonalizedTip tip = generatePersonalizedTip(user.getId(), analysis);
        
        // 알림 생성 및 저장
        LocalDateTime scheduledTime = LocalDateTime.now().plusMinutes(5); // 5분 후 발송
        LocalDateTime expireTime = LocalDateTime.now().plusHours(12); // 12시간 후 만료
        
        createAndSaveNotification(
                user,
                NotificationType.PERSONALIZED_SLEEP_TIP,
                tip.title,
                tip.message,
                scheduledTime,
                expireTime,
                Priority.NORMAL,
                null,
                null
        );
        
        log.info("개인화된 수면 팁 알림 생성 완료: 사용자 {}, 팁 유형: {}", 
                user.getId(), tip.tipType);
        
        return true;
    }

    private SleepPatternAnalysis analyzeSleepPattern(Long userId, List<SleepRecord> records) {
        log.debug("사용자 {}의 수면 패턴 분석 시작 - 기록 {}개", userId, records.size());
        
        try {
            // 1. 유효한 수면 기록만 필터링
            List<SleepRecord> validRecords = records.stream()
                    .filter(this::isValidSleepRecord)
                    .toList();
            
            if (validRecords.isEmpty()) {
                log.warn("사용자 {}의 유효한 수면 기록이 없음", userId);
                return createDefaultSleepPatternAnalysis();
            }
            
            if (validRecords.size() < records.size()) {
                log.info("사용자 {}의 수면 기록 중 {}개 제외됨 (유효하지 않음)", 
                        userId, records.size() - validRecords.size());
            }
            
            // 2. 평균 수면 시간 계산 (극단값 제외)
            double avgSleepDuration = validRecords.stream()
                    .mapToInt(SleepRecord::getTotalSleepMinutes)
                    .filter(duration -> duration >= 60 && duration <= 900) // 1-15시간 범위
                    .average()
                    .orElse(480); // 기본값 8시간
            
            // 3. 자정 넘김을 고려한 취침 시간 계산
            double avgBedtime = calculateAverageBedtime(validRecords, userId);
            
            // 4. 자정 넘김을 고려한 기상 시간 계산  
            double avgWakeTime = calculateAverageWakeTime(validRecords, userId);
            
            // 5. 일관성 점수 계산 (표준편차 기반)
            double consistencyScore = calculateSleepConsistencyScore(validRecords, userId);
            
            log.debug("사용자 {}의 수면 패턴 분석 완료: 평균 수면시간 {:.1f}분, 평균 취침 {:.1f}시, 일관성 점수 {:.1f}", 
                    userId, avgSleepDuration, avgBedtime / 60, consistencyScore);
            
            return new SleepPatternAnalysis(avgSleepDuration, avgBedtime, avgWakeTime, consistencyScore);
            
        } catch (Exception e) {
            log.error("사용자 {}의 수면 패턴 분석 중 오류 발생", userId, e);
            return createDefaultSleepPatternAnalysis();
        }
    }

    private boolean isValidSleepRecord(SleepRecord record) {
        if (record == null) return false;
        if (record.getSleepStartTime() == null || record.getSleepEndTime() == null) return false;
        if (record.getTotalSleepMinutes() <= 0) return false;
        
        // 수면 시간이 1분 이상 24시간 이하인지 확인
        if (record.getTotalSleepMinutes() < 1 || record.getTotalSleepMinutes() > 1440) {
            return false;
        }
        
        // 시작 시간이 종료 시간보다 이후인지 확인 (자정 넘김 고려)
        LocalDateTime start = record.getSleepStartTime();
        LocalDateTime end = record.getSleepEndTime();
        
        // 같은 날 시작해서 다음날 끝나는 경우는 정상
        if (start.isAfter(end) && !start.toLocalDate().equals(end.toLocalDate().minusDays(1))) {
            return false;
        }
        
        return true;
    }
    
    private SleepPatternAnalysis createDefaultSleepPatternAnalysis() {
        // 기본값 설정: 23시 취침, 7시 기상, 8시간 수면, 일관성 50점
        return new SleepPatternAnalysis(480, 23 * 60, 7 * 60, 50);
    }
    
    private double calculateAverageBedtime(List<SleepRecord> validRecords, Long userId) {
        try {
            // 취침 시간을 분으로 변환 (자정 넘김 고려)
            List<Integer> bedtimes = validRecords.stream()
                    .map(record -> {
                        LocalDateTime startTime = record.getSleepStartTime();
                        int hour = startTime.getHour();
                        int minute = startTime.getMinute();
                        
                        // 0-6시는 다음날로 간주 (24-30시로 변환)
                        if (hour >= 0 && hour <= 6) {
                            hour += 24;
                        }
                        
                        return hour * 60 + minute;
                    })
                    .toList();
            
            double avgBedtime = bedtimes.stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(23 * 60);
            
            // 24시간을 넘는 경우 다시 24시간 범위로 변환
            if (avgBedtime >= 24 * 60) {
                avgBedtime -= 24 * 60;
            }
            
            return avgBedtime;
            
        } catch (Exception e) {
            log.warn("사용자 {}의 평균 취침 시간 계산 중 오류", userId, e);
            return 23 * 60; // 기본값
        }
    }
    
    private double calculateAverageWakeTime(List<SleepRecord> validRecords, Long userId) {
        try {
            double avgWakeTime = validRecords.stream()
                    .map(record -> record.getSleepEndTime())
                    .mapToInt(endTime -> endTime.getHour() * 60 + endTime.getMinute())
                    .average()
                    .orElse(7 * 60);
                    
            return avgWakeTime;
            
        } catch (Exception e) {
            log.warn("사용자 {}의 평균 기상 시간 계산 중 오류", userId, e);
            return 7 * 60; // 기본값
        }
    }
    
    private double calculateSleepConsistencyScore(List<SleepRecord> validRecords, Long userId) {
        try {
            if (validRecords.size() < 2) {
                return 50; // 기록이 부족하면 중간 점수
            }
            
            // 취침 시간의 표준편차 계산
            List<Integer> bedtimes = validRecords.stream()
                    .map(record -> {
                        LocalDateTime startTime = record.getSleepStartTime();
                        int hour = startTime.getHour();
                        int minute = startTime.getMinute();
                        
                        // 자정 넘김 고려
                        if (hour >= 0 && hour <= 6) {
                            hour += 24;
                        }
                        
                        return hour * 60 + minute;
                    })
                    .toList();
            
            double mean = bedtimes.stream().mapToInt(Integer::intValue).average().orElse(0);
            double variance = bedtimes.stream()
                    .mapToDouble(time -> Math.pow(time - mean, 2))
                    .average()
                    .orElse(0);
            double standardDeviation = Math.sqrt(variance);
            
            // 표준편차가 작을수록 일관성이 높음 (최대 100점)
            // 1시간 이내 편차: 90-100점, 2시간 이내: 70-90점, 3시간 이내: 50-70점
            double consistencyScore;
            if (standardDeviation <= 60) { // 1시간 이내
                consistencyScore = Math.max(90, 100 - standardDeviation / 6);
            } else if (standardDeviation <= 120) { // 2시간 이내  
                consistencyScore = Math.max(70, 90 - (standardDeviation - 60) / 3);
            } else if (standardDeviation <= 180) { // 3시간 이내
                consistencyScore = Math.max(50, 70 - (standardDeviation - 120) / 6);
            } else {
                consistencyScore = Math.max(0, 50 - (standardDeviation - 180) / 12);
            }
            
            return Math.min(100, consistencyScore);
            
        } catch (Exception e) {
            log.warn("사용자 {}의 수면 일관성 점수 계산 중 오류", userId, e);
            return 50; // 기본값
        }
    }

    private PersonalizedTip generatePersonalizedTip(Long userId, SleepPatternAnalysis analysis) {
        log.debug("사용자 {}의 개인화된 팁 생성 - 수면시간: {:.1f}분, 취침시간: {:.1f}시, 일관성: {:.1f}점", 
                userId, analysis.avgSleepDuration, analysis.avgBedtime / 60, analysis.consistencyScore);
        
        try {
            // 우선순위에 따라 팁 생성 (가장 심각한 문제부터)
            
            // 1. 극단적인 수면 부족 (5시간 미만)
            if (analysis.avgSleepDuration < 300) {
                return new PersonalizedTip(
                        "SEVERE_INSUFFICIENT_SLEEP",
                        "🚨 심각한 수면 부족",
                        String.format("평균 수면시간이 %.1f시간으로 매우 부족합니다. 건강을 위해 최소 6-7시간은 주무세요. 필요시 전문의 상담을 받으시기 바랍니다.", 
                                analysis.avgSleepDuration / 60.0)
                );
            }
            
            // 2. 극단적인 과다 수면 (12시간 초과)
            if (analysis.avgSleepDuration > 720) {
                return new PersonalizedTip(
                        "EXCESSIVE_SLEEP",
                        "😴 과다 수면 주의",
                        String.format("평균 수면시간이 %.1f시간으로 너무 깁니다. 과다수면은 오히려 피로감을 증가시킬 수 있습니다. 7-9시간으로 조정해보세요.", 
                                analysis.avgSleepDuration / 60.0)
                );
            }
            
            // 3. 매우 불규칙한 수면 패턴 (일관성 30점 미만)
            if (analysis.consistencyScore < 30) {
                return new PersonalizedTip(
                        "VERY_IRREGULAR_PATTERN",
                        "⚠️ 매우 불규칙한 수면",
                        String.format("수면 패턴 일관성이 %.0f점으로 매우 불규칙합니다. 생체리듬 회복을 위해 매일 같은 시간에 잠들고 일어나는 것이 중요합니다.", 
                                analysis.consistencyScore)
                );
            }
            
            // 4. 수면 시간 부족 (7시간 미만)
            if (analysis.avgSleepDuration < 420) {
                return new PersonalizedTip(
                        "INSUFFICIENT_SLEEP",
                        "💤 수면 시간 늘리기",
                        String.format("평균 수면시간이 %.1f시간입니다. 성인 권장 수면시간(7-9시간)에 도달하기 위해 취침시간을 30분 앞당겨보세요.", 
                                analysis.avgSleepDuration / 60.0)
                );
            }
            
            // 5. 너무 늦은 취침 시간 (새벽 1시 이후)
            double bedtimeHour = analysis.avgBedtime / 60.0;
            if (bedtimeHour > 25 || (bedtimeHour >= 1 && bedtimeHour <= 6)) { // 1-6시는 25-30시로 변환됨
                int displayHour = bedtimeHour > 24 ? (int)(bedtimeHour - 24) : (int)bedtimeHour;
                return new PersonalizedTip(
                        "VERY_LATE_BEDTIME",
                        "🌛 새벽 취침 개선",
                        String.format("평균 취침시간이 새벽 %d시로 매우 늦습니다. 멜라토닌 분비와 깊은 잠을 위해 자정 전 취침을 목표로 해보세요.", displayHour)
                );
            }
            
            // 6. 늦은 취침 시간 (자정 이후)
            if (bedtimeHour > 24) {
                int displayHour = (int)(bedtimeHour - 24);
                return new PersonalizedTip(
                        "LATE_BEDTIME",
                        "🌙 일찍 잠자리에 들기",
                        String.format("평균 취침시간이 %d시로 늦습니다. 수면의 질 향상을 위해 점진적으로 15-30분씩 일찍 잠자리에 들어보세요.", displayHour)
                );
            }
            
            // 7. 불규칙한 수면 패턴 (일관성 70점 미만)
            if (analysis.consistencyScore < 70) {
                return new PersonalizedTip(
                        "IRREGULAR_PATTERN",
                        "⏰ 규칙적인 수면 패턴",
                        String.format("수면 패턴 일관성이 %.0f점입니다. 매일 같은 시간에 잠들고 일어나는 습관으로 생체리듬을 안정시켜보세요.", 
                                analysis.consistencyScore)
                );
            }
            
            // 8. 적당한 과다 수면 (9시간 초과)
            if (analysis.avgSleepDuration > 540) {
                return new PersonalizedTip(
                        "MILD_EXCESSIVE_SLEEP",
                        "☀️ 적정 수면 시간 조정",
                        String.format("평균 수면시간이 %.1f시간입니다. 수면의 질을 높이기 위해 8시간 내외로 조정해보는 것은 어떨까요?", 
                                analysis.avgSleepDuration / 60.0)
                );
            }
            
            // 9. 수면 패턴이 양호한 경우 - 일반적인 수면 팁
            return generateGeneralSleepTip(userId, analysis);
            
        } catch (Exception e) {
            log.error("사용자 {}의 개인화된 팁 생성 중 오류", userId, e);
            return generateGeneralSleepTip(userId, analysis);
        }
    }
    
    private PersonalizedTip generateGeneralSleepTip(Long userId, SleepPatternAnalysis analysis) {
        log.debug("사용자 {}에게 일반적인 수면 팁 제공", userId);
        
        // 시간대나 계절에 따른 다양한 팁 제공
        String[] generalTips = {
                "잠들기 1시간 전에는 스마트폰과 밝은 화면을 피해보세요 📱",
                "침실 온도를 18-20도로 유지하면 더 깊은 잠을 잘 수 있어요 🌡️", 
                "카페인은 오후 2시 이후 피하면 밤잠에 도움이 됩니다 ☕",
                "규칙적인 운동은 수면의 질을 높여줍니다 (단, 잠들기 3시간 전까지) 🏃‍♂️",
                "잠들기 전 따뜻한 목욕이나 차분한 독서로 마음을 진정시켜보세요 📚",
                "침실은 최대한 어둡고 조용하게 유지해보세요 🌙",
                "침대는 수면과 휴식 용도로만 사용하는 것이 좋습니다 🛏️",
                "취침 2시간 전부터는 과식을 피하고 가벼운 간식만 드세요 🍎",
                "라벤더 향이나 은은한 아로마로 수면 환경을 조성해보세요 🌸",
                "잠들기 전 간단한 명상이나 깊은 호흡으로 스트레스를 풀어보세요 🧘‍♀️"
        };
        
        // 현재 시간을 기반으로 적절한 팁 선택 (의사 랜덤)
        int tipIndex = (int)((userId + LocalDateTime.now().getDayOfMonth()) % generalTips.length);
        
        return new PersonalizedTip(
                "GENERAL_TIP",
                "💡 오늘의 수면 팁",
                generalTips[tipIndex]
        );
    }

    // === 내부 클래스들 ===
    
    private static class SleepPatternAnalysis {
        final double avgSleepDuration;
        final double avgBedtime;
        final double avgWakeTime;
        final double consistencyScore;
        
        SleepPatternAnalysis(double avgSleepDuration, double avgBedtime, double avgWakeTime, double consistencyScore) {
            this.avgSleepDuration = avgSleepDuration;
            this.avgBedtime = avgBedtime;
            this.avgWakeTime = avgWakeTime;
            this.consistencyScore = consistencyScore;
        }
    }
    
    private static class PersonalizedTip {
        final String tipType;
        final String title;
        final String message;
        
        PersonalizedTip(String tipType, String title, String message) {
            this.tipType = tipType;
            this.title = title;
            this.message = message;
        }
    }

    // === 헬퍼 메서드들 ===

    private Notification createAndSaveNotification(User user, NotificationType type, String title, String message,
                                                  LocalDateTime scheduledAt, LocalDateTime expiresAt, Priority priority,
                                                  Long relatedDataId, String relatedDataType) {

        Priority finalPriority = (priority != null) ? priority : Priority.NORMAL;

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .scheduledAt(scheduledAt)
                .expiresAt(expiresAt)
                .priority(finalPriority)
                .relatedDataId(relatedDataId)
                .relatedDataType(relatedDataType)
                .build();
        return notificationRepository.save(notification);
    }

    private void sendPushNotificationIfTokenExists(User user, String title, String body) {
        if (!StringUtils.hasText(user.getFcmToken())) {
            log.debug("FCM 토큰이 없어 푸시 알림을 건너뜁니다: 사용자 ID {}", user.getId());
            return;
        }

        // FcmPushService가 사용 가능한 경우에만 푸시 알림 발송
        if (fcmPushService != null) {
            try {
                // 사용자에게 푸시 알림 발송
                fcmPushService.sendToUser(user.getId(), title, body, null);
                log.info("FCM 푸시 알림 발송 요청 완료: 사용자 ID {}, 제목: {}", user.getId(), title);
            } catch (Exception e) {
                log.error("FCM 푸시 알림 발송 실패: 사용자 ID {}, 오류: {}", user.getId(), e.getMessage(), e);
            }
        } else {
            log.info("FcmPushService가 설정되지 않아 푸시 알림을 건너뜁니다: 사용자 ID {}", user.getId());
        }
    }


    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }
} 