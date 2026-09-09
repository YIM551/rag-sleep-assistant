package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.entity.UserSleepSetting;
import java.time.LocalTime;
import java.util.List;

/**
 * 개인화된 스케줄링 서비스 인터페이스
 * 
 * 사용자별 맞춤 수면 알림 스케줄링을 담당하는 서비스입니다.
 * 기존의 하드코딩된 스케줄러를 대체하여 동적 스케줄링을 제공합니다.
 * 
 * 주요 기능:
 * - 사용자 설정 기반 동적 스케줄링
 * - 평일/주말 구분 스케줄링
 * - 스마트 타이밍 계산 통합
 * - 실시간 스케줄 업데이트
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
public interface PersonalizedSchedulerService {

    // === 메인 스케줄링 메서드들 ===

    /**
     * 모든 사용자의 개인화된 수면 알림을 스케줄링합니다.
     * 기존 NotificationScheduler의 하드코딩된 메서드를 대체합니다.
     */
    void scheduleAllPersonalizedSleepReminders();

    /**
     * 특정 사용자의 개인화된 수면 알림을 스케줄링합니다.
     * 
     * @param userId 사용자 ID
     */
    void schedulePersonalizedSleepRemindersForUser(Long userId);

    /**
     * 모든 사용자의 기상 알림을 스케줄링합니다.
     */
    void scheduleAllPersonalizedWakeUpReminders();

    /**
     * 특정 사용자의 기상 알림을 스케줄링합니다.
     * 
     * @param userId 사용자 ID
     */
    void schedulePersonalizedWakeUpRemindersForUser(Long userId);

    // === 시간대별 사용자 조회 메서드들 ===

    /**
     * 특정 시간대에 수면 알림을 받아야 하는 사용자들을 조회합니다.
     * 
     * @param reminderTime 알림 시간
     * @param isWeekend 주말 여부
     * @return 해당 시간대의 사용자 설정 목록
     */
    List<UserSleepSetting> getUsersForSleepReminder(LocalTime reminderTime, boolean isWeekend);

    /**
     * 특정 시간대에 기상 알림을 받아야 하는 사용자들을 조회합니다.
     * 
     * @param reminderTime 알림 시간
     * @param isWeekend 주말 여부
     * @return 해당 시간대의 사용자 설정 목록
     */
    List<UserSleepSetting> getUsersForWakeUpReminder(LocalTime reminderTime, boolean isWeekend);

    // === 스케줄 관리 메서드들 ===

    /**
     * 사용자의 설정 변경 시 해당 사용자의 스케줄을 업데이트합니다.
     * 
     * @param userId 사용자 ID
     */
    void updateUserSchedule(Long userId);

    /**
     * 사용자의 스케줄을 제거합니다. (계정 비활성화, 알림 비활성화 시)
     * 
     * @param userId 사용자 ID
     */
    void removeUserSchedule(Long userId);

    /**
     * 모든 사용자의 스케줄을 재설정합니다.
     * 시스템 재시작 시나 설정 대대적 변경 시 사용
     */
    void refreshAllSchedules();

    // === 스마트 타이밍 통합 메서드들 ===

    /**
     * 스마트 타이밍이 활성화된 사용자들의 알림 시간을 재계산하고 업데이트합니다.
     */
    void updateSmartTimingForAllUsers();

    /**
     * 특정 사용자의 스마트 타이밍을 재계산하고 스케줄을 업데이트합니다.
     * 
     * @param userId 사용자 ID
     */
    void updateSmartTimingForUser(Long userId);

    // === 스케줄 상태 조회 메서드들 ===

    /**
     * 현재 활성화된 모든 스케줄의 개수를 조회합니다.
     * 
     * @return 활성 스케줄 개수
     */
    int getActiveScheduleCount();

    /**
     * 특정 사용자의 활성 스케줄 개수를 조회합니다.
     * 
     * @param userId 사용자 ID
     * @return 해당 사용자의 활성 스케줄 개수
     */
    int getUserActiveScheduleCount(Long userId);

    /**
     * 시스템의 다음 예정된 알림 시간을 조회합니다.
     * 
     * @return 다음 알림 예정 시간들의 목록
     */
    List<LocalTime> getUpcomingReminderTimes();
}