package com.sleepwell.sleepwell_backend.dto.sleep;

import com.sleepwell.sleepwell_backend.entity.UserSleepSetting;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * 사용자 수면 설정 조회용 DTO
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSleepSettingDto {

    private Long id;
    private Long userId;
    private Integer sleepReminderMinutesBefore;
    private LocalTime phoneInactiveStartTime;
    private LocalTime phoneInactiveEndTime;
    private Boolean weekendDifferentSchedule;
    private LocalTime weekendBedtime;
    private LocalTime weekendWakeupTime;
    private Boolean smartTimingEnabled;
    private Integer consecutiveIgnoredNotifications;
    private Integer mostEffectiveReminderMinutes;
    private Boolean sleepReminderEnabled;
    private Boolean wakeupReminderEnabled;
    private String notificationSound;

    /**
     * Entity를 DTO로 변환하는 생성자
     */
    public UserSleepSettingDto(UserSleepSetting entity) {
        this.id = entity.getId();
        this.userId = entity.getUser().getId();
        this.sleepReminderMinutesBefore = entity.getSleepReminderMinutesBefore();
        this.phoneInactiveStartTime = entity.getPhoneInactiveStartTime();
        this.phoneInactiveEndTime = entity.getPhoneInactiveEndTime();
        this.weekendDifferentSchedule = entity.getWeekendDifferentSchedule();
        this.weekendBedtime = entity.getWeekendBedtime();
        this.weekendWakeupTime = entity.getWeekendWakeupTime();
        this.smartTimingEnabled = entity.getSmartTimingEnabled();
        this.consecutiveIgnoredNotifications = entity.getConsecutiveIgnoredNotifications();
        this.mostEffectiveReminderMinutes = entity.getMostEffectiveReminderMinutes();
        this.sleepReminderEnabled = entity.getSleepReminderEnabled();
        this.wakeupReminderEnabled = entity.getWakeupReminderEnabled();
        this.notificationSound = entity.getNotificationSound();
    }
}