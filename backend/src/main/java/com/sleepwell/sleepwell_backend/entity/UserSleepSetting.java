package com.sleepwell.sleepwell_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * 사용자별 수면 설정 엔티티
 * 
 * 개인화된 수면 알림 및 스케줄링을 위한 사용자 설정을 관리하는 엔티티입니다.
 * User 엔티티와 분리하여 설정의 확장성과 성능을 확보했습니다.
 * 
 * 주요 기능:
 * - 개인화된 수면 알림 시간 설정
 * - 스마트폰 사용 패턴 기반 알림 타이밍 최적화
 * - 평일/주말 다른 스케줄 지원
 * - 스마트 타이밍 학습 기능
 * 
 * 설계 근거:
 * - Slack의 사례를 참고하여 User 테이블 분리로 성능 최적화
 * - 향후 ASMR, AI 추천 등 기능 확장 대비
 * - 설정별 개별 접근 및 캐싱 최적화
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(name = "user_sleep_settings", indexes = {
    @Index(name = "IDX_SLEEP_SETTING_USER", columnList = "user_id", unique = true),
    @Index(name = "IDX_SLEEP_SETTING_SMART_TIMING", columnList = "smartTimingEnabled, user_id"),
    @Index(name = "IDX_SLEEP_SETTING_WEEKEND", columnList = "weekendDifferentSchedule, user_id")
})
public class UserSleepSetting extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 설정 소유 사용자 (1:1 관계)
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * 수면 알림을 몇 분 전에 받을지 설정 (기본값: 60분)
     */
    @Builder.Default
    @Column(nullable = false)
    private Integer sleepReminderMinutesBefore = 60;

    /**
     * 스마트폰 사용을 줄이기 시작하는 시간
     */
    private LocalTime phoneInactiveStartTime;

    /**
     * 스마트폰 사용을 다시 시작하는 시간
     */
    private LocalTime phoneInactiveEndTime;

    /**
     * 주말에 다른 스케줄을 사용할지 여부
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean weekendDifferentSchedule = false;

    /**
     * 주말 취침 시간
     */
    private LocalTime weekendBedtime;

    /**
     * 주말 기상 시간
     */
    private LocalTime weekendWakeupTime;

    /**
     * 스마트 타이밍 기능 활성화 여부
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean smartTimingEnabled = false;

    /**
     * 연속으로 알림을 무시한 횟수
     */
    @Builder.Default
    @Column(nullable = false)
    private Integer consecutiveIgnoredNotifications = 0;

    /**
     * 가장 효과적이었던 알림 시간 (분 단위)
     */
    private Integer mostEffectiveReminderMinutes;

    /**
     * 수면 준비 알림 활성화 여부
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean sleepReminderEnabled = true;

    /**
     * 기상 알림 활성화 여부
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean wakeupReminderEnabled = true;

    /**
     * 알림 톤/사운드 설정
     */
    @Builder.Default
    @Column(length = 50)
    private String notificationSound = "default";

    // === 비즈니스 메서드 ===

    /**
     * 현재 요일에 맞는 취침 시간 반환
     */
    public LocalTime getApplicableBedtime(boolean isWeekend) {
        if (weekendDifferentSchedule && isWeekend && weekendBedtime != null) {
            return weekendBedtime;
        }
        return user.getPreferredBedtime();
    }

    /**
     * 현재 요일에 맞는 기상 시간 반환
     */
    public LocalTime getApplicableWakeupTime(boolean isWeekend) {
        if (weekendDifferentSchedule && isWeekend && weekendWakeupTime != null) {
            return weekendWakeupTime;
        }
        return user.getPreferredWakeupTime();
    }

    /**
     * 스마트 타이밍이 활성화되어 있고 학습 데이터가 있는지 확인
     */
    public boolean hasSmartTimingData() {
        return smartTimingEnabled && mostEffectiveReminderMinutes != null;
    }

    /**
     * 가장 효과적인 알림 시간을 반환
     */
    public Integer getEffectiveReminderMinutes() {
        if (hasSmartTimingData()) {
            return mostEffectiveReminderMinutes;
        }
        return sleepReminderMinutesBefore;
    }

    /**
     * 알림 무시 횟수 증가
     */
    public void incrementIgnoredNotifications() {
        this.consecutiveIgnoredNotifications++;
    }

    /**
     * 알림에 반응했을 때 호출
     */
    public void resetIgnoredNotificationsAndLearn(Integer effectiveMinutes) {
        this.consecutiveIgnoredNotifications = 0;
        if (effectiveMinutes != null) {
            this.mostEffectiveReminderMinutes = effectiveMinutes;
        }
    }

    /**
     * 폰 비활성 시간대인지 확인
     */
    public boolean isPhoneInactiveTime(LocalTime currentTime) {
        if (phoneInactiveStartTime == null || phoneInactiveEndTime == null) {
            return false;
        }
        
        if (phoneInactiveStartTime.isAfter(phoneInactiveEndTime)) {
            return currentTime.isAfter(phoneInactiveStartTime) || currentTime.isBefore(phoneInactiveEndTime);
        } else {
            return currentTime.isAfter(phoneInactiveStartTime) && currentTime.isBefore(phoneInactiveEndTime);
        }
    }

    /**
     * 현재 요일에 맞는 취침 알림 시간 반환 (알림 분수 고려)
     */
    public LocalTime getBedtimeReminderTime() {
        LocalTime bedtime = getApplicableBedtime(false); // 평일 기준
        if (bedtime == null) {
            return null;
        }
        return bedtime.minusMinutes(getEffectiveReminderMinutes());
    }

    /**
     * 주말 취침 알림 시간 반환
     */
    public LocalTime getWeekendBedtimeReminderTime() {
        if (!weekendDifferentSchedule) {
            return getBedtimeReminderTime();
        }
        LocalTime bedtime = getApplicableBedtime(true); // 주말 기준
        if (bedtime == null) {
            return null;
        }
        return bedtime.minusMinutes(getEffectiveReminderMinutes());
    }

    /**
     * 현재 요일에 맞는 기상 알림 시간 반환
     */
    public LocalTime getWakeUpReminderTime() {
        return getApplicableWakeupTime(false); // 평일 기준
    }

    /**
     * 주말 기상 알림 시간 반환
     */
    public LocalTime getWeekendWakeUpReminderTime() {
        if (!weekendDifferentSchedule) {
            return getWakeUpReminderTime();
        }
        return getApplicableWakeupTime(true); // 주말 기준
    }

    /**
     * 설정 업데이트
     */
    public void updateSettings(Integer reminderMinutes, LocalTime phoneInactiveStart, LocalTime phoneInactiveEnd,
                               Boolean weekendSchedule, LocalTime weekendBed, LocalTime weekendWakeup,
                               Boolean smartTiming, Boolean sleepReminder, Boolean wakeupReminder,
                               String sound) {
        if (reminderMinutes != null && reminderMinutes > 0) {
            this.sleepReminderMinutesBefore = reminderMinutes;
        }
        this.phoneInactiveStartTime = phoneInactiveStart;
        this.phoneInactiveEndTime = phoneInactiveEnd;
        
        if (weekendSchedule != null) {
            this.weekendDifferentSchedule = weekendSchedule;
        }
        this.weekendBedtime = weekendBed;
        this.weekendWakeupTime = weekendWakeup;
        
        if (smartTiming != null) {
            this.smartTimingEnabled = smartTiming;
        }
        if (sleepReminder != null) {
            this.sleepReminderEnabled = sleepReminder;
        }
        if (wakeupReminder != null) {
            this.wakeupReminderEnabled = wakeupReminder;
        }
        if (sound != null && !sound.trim().isEmpty()) {
            this.notificationSound = sound;
        }
    }
}