package com.sleepwell.sleepwell_backend.service.impl;

import com.sleepwell.sleepwell_backend.entity.UserSleepSetting;
import com.sleepwell.sleepwell_backend.enums.NotificationType;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.exception.ErrorCode;
import com.sleepwell.sleepwell_backend.repository.UserSleepSettingRepository;
import com.sleepwell.sleepwell_backend.service.NotificationService;
import com.sleepwell.sleepwell_backend.service.PersonalizedSchedulerService;
import com.sleepwell.sleepwell_backend.service.PersonalizedTimingCalculator;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

/**
 * 개인화된 스케줄링 서비스 구현체
 * 
 * 사용자별 맞춤 수면 알림을 동적으로 스케줄링하는 서비스입니다.
 * 기존의 하드코딩된 @Scheduled 어노테이션을 대체하여 유연한 스케줄링을 제공합니다.
 * 
 * 주요 특징:
 * - TaskScheduler를 사용한 동적 스케줄링
 * - 사용자별 개별 스케줄 관리
 * - 평일/주말 구분 스케줄링
 * - 실시간 스케줄 업데이트
 * - 메모리 효율적인 스케줄 관리
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class PersonalizedSchedulerServiceImpl implements PersonalizedSchedulerService {

    private final UserSleepSettingRepository userSleepSettingRepository;
    private final NotificationService notificationService;
    private final TaskScheduler taskScheduler;
    
    // 김영한 방식: @Lazy로 순환 의존성 해결
    private final PersonalizedTimingCalculator timingCalculator;
    
    // 생성자 주입
    public PersonalizedSchedulerServiceImpl(
            UserSleepSettingRepository userSleepSettingRepository,
            NotificationService notificationService,
            TaskScheduler taskScheduler,
            @Lazy PersonalizedTimingCalculator timingCalculator) {
        this.userSleepSettingRepository = userSleepSettingRepository;
        this.notificationService = notificationService;
        this.taskScheduler = taskScheduler;
        this.timingCalculator = timingCalculator;
    }
    
    @PostConstruct
    public void init() {
        log.info("PersonalizedSchedulerServiceImpl 초기화 완료 - 순환 의존성 해결됨");
        log.info("PersonalizedTimingCalculator 로딩: {}", 
                timingCalculator != null ? "성공" : "실패");
    }

    // 스케줄 관리를 위한 메모리 저장소
    private final Map<String, ScheduledFuture<?>> activeSchedules = new ConcurrentHashMap<>();
    
    // 시간 포맷터
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    @Transactional
    public void scheduleAllPersonalizedSleepReminders() {
        log.info("모든 사용자의 개인화된 수면 알림 스케줄링 시작");
        
        try {
            // 기존 수면 알림 스케줄 정리
            removeAllSleepReminderSchedules();
            
            // 수면 알림이 활성화된 모든 사용자 조회
            List<UserSleepSetting> activeSettings = userSleepSettingRepository.findAllWithSleepReminderEnabled();
            
            log.info("수면 알림 활성 사용자 {}명 발견", activeSettings.size());
            
            // 사용자별 스케줄링
            for (UserSleepSetting setting : activeSettings) {
                try {
                    schedulePersonalizedSleepRemindersForUser(setting.getUser().getId());
                } catch (Exception e) {
                    log.error("사용자 {}의 수면 알림 스케줄링 실패", setting.getUser().getId(), e);
                }
            }
            
            log.info("개인화된 수면 알림 스케줄링 완료. 총 {}개 스케줄 생성", activeSchedules.size());
            
        } catch (Exception e) {
            log.error("수면 알림 스케줄링 중 오류 발생", e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public void schedulePersonalizedSleepRemindersForUser(Long userId) {
        log.debug("사용자 {}의 개인화된 수면 알림 스케줄링", userId);
        
        UserSleepSetting setting = userSleepSettingRepository.findByUserIdWithUser(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SLEEP_SETTING_NOT_FOUND));
        
        if (!setting.getSleepReminderEnabled() || !setting.getUser().getIsActive() || !setting.getUser().getNotificationEnabled()) {
            log.debug("사용자 {}는 수면 알림 비활성화 상태", userId);
            return;
        }
        
        // 기존 스케줄 제거
        removeUserSleepReminderSchedules(userId);
        
        // 평일 스케줄 생성
        scheduleWeekdaySleepReminder(setting);
        
        // 주말 스케줄 생성 (주말 다른 스케줄 사용 시)
        if (setting.getWeekendDifferentSchedule()) {
            scheduleWeekendSleepReminder(setting);
        }
        
        log.debug("사용자 {}의 수면 알림 스케줄링 완료", userId);
    }

    @Override
    @Transactional
    public void scheduleAllPersonalizedWakeUpReminders() {
        log.info("모든 사용자의 개인화된 기상 알림 스케줄링 시작");
        
        try {
            // 기존 기상 알림 스케줄 정리
            removeAllWakeUpReminderSchedules();
            
            // 기상 알림이 활성화된 모든 사용자 조회
            List<UserSleepSetting> activeSettings = userSleepSettingRepository.findAllWithWakeupReminderEnabled();
            
            log.info("기상 알림 활성 사용자 {}명 발견", activeSettings.size());
            
            // 사용자별 스케줄링
            for (UserSleepSetting setting : activeSettings) {
                try {
                    schedulePersonalizedWakeUpRemindersForUser(setting.getUser().getId());
                } catch (Exception e) {
                    log.error("사용자 {}의 기상 알림 스케줄링 실패", setting.getUser().getId(), e);
                }
            }
            
            log.info("개인화된 기상 알림 스케줄링 완료");
            
        } catch (Exception e) {
            log.error("기상 알림 스케줄링 중 오류 발생", e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public void schedulePersonalizedWakeUpRemindersForUser(Long userId) {
        log.debug("사용자 {}의 개인화된 기상 알림 스케줄링", userId);
        
        UserSleepSetting setting = userSleepSettingRepository.findByUserIdWithUser(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SLEEP_SETTING_NOT_FOUND));
        
        if (!setting.getWakeupReminderEnabled() || !setting.getUser().getIsActive() || !setting.getUser().getNotificationEnabled()) {
            log.debug("사용자 {}는 기상 알림 비활성화 상태", userId);
            return;
        }
        
        // 기존 스케줄 제거
        removeUserWakeUpReminderSchedules(userId);
        
        // 평일 스케줄 생성
        scheduleWeekdayWakeUpReminder(setting);
        
        // 주말 스케줄 생성 (주말 다른 스케줄 사용 시)
        if (setting.getWeekendDifferentSchedule()) {
            scheduleWeekendWakeUpReminder(setting);
        }
        
        log.debug("사용자 {}의 기상 알림 스케줄링 완료", userId);
    }

    @Override
    public List<UserSleepSetting> getUsersForSleepReminder(LocalTime reminderTime, boolean isWeekend) {
        if (isWeekend) {
            return userSleepSettingRepository.findUsersForWeekendReminder(reminderTime);
        } else {
            return userSleepSettingRepository.findUsersForWeekdayReminder(reminderTime);
        }
    }

    @Override
    public List<UserSleepSetting> getUsersForWakeUpReminder(LocalTime reminderTime, boolean isWeekend) {
        if (isWeekend) {
            return userSleepSettingRepository.findUsersForWeekendWakeUpReminder(reminderTime);
        } else {
            return userSleepSettingRepository.findUsersForWeekdayWakeUpReminder(reminderTime);
        }
    }

    @Override
    @Transactional
    public void updateUserSchedule(Long userId) {
        log.info("사용자 {}의 스케줄 업데이트", userId);
        
        // 기존 스케줄 제거
        removeUserSchedule(userId);
        
        // 새 스케줄 생성
        schedulePersonalizedSleepRemindersForUser(userId);
        schedulePersonalizedWakeUpRemindersForUser(userId);
        
        log.info("사용자 {}의 스케줄 업데이트 완료", userId);
    }

    @Override
    public void removeUserSchedule(Long userId) {
        log.debug("사용자 {}의 스케줄 제거", userId);
        
        removeUserSleepReminderSchedules(userId);
        removeUserWakeUpReminderSchedules(userId);
        
        log.debug("사용자 {}의 스케줄 제거 완료", userId);
    }

    @Override
    @Transactional
    public void refreshAllSchedules() {
        log.info("모든 스케줄 재설정 시작");
        
        // 모든 스케줄 제거
        activeSchedules.values().forEach(future -> future.cancel(false));
        activeSchedules.clear();
        
        // 새로 스케줄링
        scheduleAllPersonalizedSleepReminders();
        scheduleAllPersonalizedWakeUpReminders();
        
        log.info("모든 스케줄 재설정 완료");
    }

    @Override
    public void updateSmartTimingForAllUsers() {
        log.info("모든 사용자의 스마트 타이밍 업데이트 시작");
        
        List<UserSleepSetting> smartTimingUsers = userSleepSettingRepository.findAllWithSmartTimingEnabled();
        
        for (UserSleepSetting setting : smartTimingUsers) {
            try {
                updateSmartTimingForUser(setting.getUser().getId());
            } catch (Exception e) {
                log.error("사용자 {}의 스마트 타이밍 업데이트 실패", setting.getUser().getId(), e);
            }
        }
        
        log.info("스마트 타이밍 업데이트 완료. 대상 사용자: {}명", smartTimingUsers.size());
    }

    @Override
    @Transactional
    public void updateSmartTimingForUser(Long userId) {
        log.debug("사용자 {}의 스마트 타이밍 업데이트", userId);
        
        try {
            // PersonalizedTimingCalculator를 사용한 스마트 타이밍 재계산
            timingCalculator.recalculateUserTiming(userId);
            
            // 재계산된 설정으로 스케줄 업데이트
            updateUserSchedule(userId);
            
            log.debug("사용자 {}의 스마트 타이밍 업데이트 완료", userId);
            
        } catch (Exception e) {
            log.error("사용자 {}의 스마트 타이밍 업데이트 실패", userId, e);
            // 실패 시에도 기본 스케줄은 유지
            updateUserSchedule(userId);
        }
    }

    @Override
    public int getActiveScheduleCount() {
        return activeSchedules.size();
    }

    @Override
    public int getUserActiveScheduleCount(Long userId) {
        String userPrefix = "user_" + userId + "_";
        return (int) activeSchedules.keySet().stream()
            .filter(key -> key.startsWith(userPrefix))
            .count();
    }

    @Override
    public List<LocalTime> getUpcomingReminderTimes() {
        // 현재 시간부터 다음 24시간 내의 알림 시간들을 조회
        return userSleepSettingRepository.findAllWithSleepReminderEnabled().stream()
            .map(setting -> setting.getBedtimeReminderTime())
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    // === 내부 헬퍼 메서드들 ===

    private void scheduleWeekdaySleepReminder(UserSleepSetting setting) {
        LocalTime reminderTime = setting.getBedtimeReminderTime();
        if (reminderTime == null) {
            log.warn("사용자 {}의 평일 수면 알림 시간이 설정되지 않음", setting.getUser().getId());
            return;
        }
        
        // 평일 크론 표현식 생성 (월-금)
        String cronExpression = String.format("0 %d %d * * MON-FRI", 
            reminderTime.getMinute(), reminderTime.getHour());
        
        scheduleNotification(
            generateScheduleKey(setting.getUser().getId(), "sleep", "weekday"),
            cronExpression,
            () -> sendSleepReminder(setting.getUser().getId())
        );
        
        log.debug("사용자 {}의 평일 수면 알림 스케줄 생성: {}", setting.getUser().getId(), cronExpression);
    }

    private void scheduleWeekendSleepReminder(UserSleepSetting setting) {
        LocalTime reminderTime = setting.getWeekendBedtimeReminderTime();
        if (reminderTime == null) {
            log.warn("사용자 {}의 주말 수면 알림 시간이 설정되지 않음", setting.getUser().getId());
            return;
        }
        
        // 주말 크론 표현식 생성 (토-일)
        String cronExpression = String.format("0 %d %d * * SAT-SUN", 
            reminderTime.getMinute(), reminderTime.getHour());
        
        scheduleNotification(
            generateScheduleKey(setting.getUser().getId(), "sleep", "weekend"),
            cronExpression,
            () -> sendSleepReminder(setting.getUser().getId())
        );
        
        log.debug("사용자 {}의 주말 수면 알림 스케줄 생성: {}", setting.getUser().getId(), cronExpression);
    }

    private void scheduleWeekdayWakeUpReminder(UserSleepSetting setting) {
        LocalTime reminderTime = setting.getWakeUpReminderTime();
        if (reminderTime == null) {
            log.warn("사용자 {}의 평일 기상 알림 시간이 설정되지 않음", setting.getUser().getId());
            return;
        }
        
        // 평일 크론 표현식 생성 (월-금)
        String cronExpression = String.format("0 %d %d * * MON-FRI", 
            reminderTime.getMinute(), reminderTime.getHour());
        
        scheduleNotification(
            generateScheduleKey(setting.getUser().getId(), "wakeup", "weekday"),
            cronExpression,
            () -> sendWakeUpReminder(setting.getUser().getId())
        );
        
        log.debug("사용자 {}의 평일 기상 알림 스케줄 생성: {}", setting.getUser().getId(), cronExpression);
    }

    private void scheduleWeekendWakeUpReminder(UserSleepSetting setting) {
        LocalTime reminderTime = setting.getWeekendWakeUpReminderTime();
        if (reminderTime == null) {
            log.warn("사용자 {}의 주말 기상 알림 시간이 설정되지 않음", setting.getUser().getId());
            return;
        }
        
        // 주말 크론 표현식 생성 (토-일)
        String cronExpression = String.format("0 %d %d * * SAT-SUN", 
            reminderTime.getMinute(), reminderTime.getHour());
        
        scheduleNotification(
            generateScheduleKey(setting.getUser().getId(), "wakeup", "weekend"),
            cronExpression,
            () -> sendWakeUpReminder(setting.getUser().getId())
        );
        
        log.debug("사용자 {}의 주말 기상 알림 스케줄 생성: {}", setting.getUser().getId(), cronExpression);
    }

    private void scheduleNotification(String scheduleKey, String cronExpression, Runnable task) {
        try {
            ScheduledFuture<?> future = taskScheduler.schedule(task, new CronTrigger(cronExpression));
            activeSchedules.put(scheduleKey, future);
            
            log.debug("스케줄 등록 완료: {} - {}", scheduleKey, cronExpression);
            
        } catch (Exception e) {
            log.error("스케줄 등록 실패: {} - {}", scheduleKey, cronExpression, e);
        }
    }

    private void sendSleepReminder(Long userId) {
        try {
            log.debug("사용자 {}에게 수면 알림 발송", userId);
            
            UserSleepSetting setting = userSleepSettingRepository.findByUserIdWithUser(userId)
                .orElse(null);
            
            if (setting == null || !setting.getSleepReminderEnabled()) {
                log.debug("사용자 {}의 수면 알림이 비활성화됨", userId);
                return;
            }
            
            // 개인화된 메시지 생성
            String title = "💤 수면 준비 시간이에요";
            String message = String.format("%s님, 좋은 잠을 위한 준비를 시작해보세요!", 
                setting.getUser().getName());
            
            notificationService.createAndSendNotification(userId, NotificationType.SLEEP_REMINDER, title, message);
            
            log.debug("사용자 {}에게 수면 알림 발송 완료", userId);
            
        } catch (Exception e) {
            log.error("사용자 {}의 수면 알림 발송 실패", userId, e);
        }
    }

    private void sendWakeUpReminder(Long userId) {
        try {
            log.debug("사용자 {}에게 기상 알림 발송", userId);
            
            UserSleepSetting setting = userSleepSettingRepository.findByUserIdWithUser(userId)
                .orElse(null);
            
            if (setting == null || !setting.getWakeupReminderEnabled()) {
                log.debug("사용자 {}의 기상 알림이 비활성화됨", userId);
                return;
            }
            
            // 개인화된 메시지 생성
            String title = "🌅 상쾌한 아침이에요";
            String message = String.format("%s님, 좋은 아침입니다! 활기찬 하루를 시작해보세요!", 
                setting.getUser().getName());
            
            notificationService.createAndSendNotification(userId, NotificationType.WAKE_UP_REMINDER, title, message);
            
            log.debug("사용자 {}에게 기상 알림 발송 완료", userId);
            
        } catch (Exception e) {
            log.error("사용자 {}의 기상 알림 발송 실패", userId, e);
        }
    }

    private String generateScheduleKey(Long userId, String type, String period) {
        return String.format("user_%d_%s_%s", userId, type, period);
    }

    private void removeUserSleepReminderSchedules(Long userId) {
        removeScheduleByPattern("user_" + userId + "_sleep_");
    }

    private void removeUserWakeUpReminderSchedules(Long userId) {
        removeScheduleByPattern("user_" + userId + "_wakeup_");
    }

    private void removeAllSleepReminderSchedules() {
        removeScheduleByPattern("_sleep_");
    }

    private void removeAllWakeUpReminderSchedules() {
        removeScheduleByPattern("_wakeup_");
    }

    private void removeScheduleByPattern(String pattern) {
        List<String> keysToRemove = activeSchedules.keySet().stream()
            .filter(key -> key.contains(pattern))
            .collect(Collectors.toList());
        
        for (String key : keysToRemove) {
            ScheduledFuture<?> future = activeSchedules.remove(key);
            if (future != null) {
                future.cancel(false);
                log.debug("스케줄 제거 완료: {}", key);
            }
        }
    }
}