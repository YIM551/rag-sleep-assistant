package com.sleepwell.sleepwell_backend.repository;

import com.sleepwell.sleepwell_backend.entity.UserSleepSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 수면 설정 Repository
 * 
 * 개인화된 수면 알림 및 스케줄링을 위한 데이터 접근 계층입니다.
 * 성능 최적화를 위한 다양한 쿼리 메서드를 제공합니다.
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
@Repository
public interface UserSleepSettingRepository extends JpaRepository<UserSleepSetting, Long> {

    /**
     * 사용자 ID로 수면 설정 조회
     */
    Optional<UserSleepSetting> findByUserId(Long userId);

    /**
     * 사용자 ID로 수면 설정 존재 여부 확인
     */
    boolean existsByUserId(Long userId);

    /**
     * 수면 알림이 활성화된 모든 사용자의 설정 조회
     */
    @Query("SELECT uss FROM UserSleepSetting uss " +
           "JOIN FETCH uss.user u " +
           "WHERE uss.sleepReminderEnabled = true " +
           "AND u.isActive = true " +
           "AND u.notificationEnabled = true")
    List<UserSleepSetting> findAllWithSleepReminderEnabled();

    /**
     * 스마트 타이밍이 활성화된 사용자 설정 조회
     */
    @Query("SELECT uss FROM UserSleepSetting uss " +
           "JOIN FETCH uss.user u " +
           "WHERE uss.smartTimingEnabled = true " +
           "AND uss.sleepReminderEnabled = true " +
           "AND u.isActive = true")
    List<UserSleepSetting> findAllWithSmartTimingEnabled();

    /**
     * 주말 다른 스케줄을 사용하는 사용자 설정 조회
     */
    @Query("SELECT uss FROM UserSleepSetting uss " +
           "JOIN FETCH uss.user u " +
           "WHERE uss.weekendDifferentSchedule = true " +
           "AND uss.sleepReminderEnabled = true " +
           "AND u.isActive = true")
    List<UserSleepSetting> findAllWithWeekendDifferentSchedule();

    /**
     * User 엔티티와 함께 사용자 설정 조회 (N+1 문제 방지)
     */
    @Query("SELECT uss FROM UserSleepSetting uss " +
           "JOIN FETCH uss.user u " +
           "WHERE uss.user.id = :userId")
    Optional<UserSleepSetting> findByUserIdWithUser(@Param("userId") Long userId);

    /**
     * 활성 사용자의 모든 수면 설정 조회
     */
    @Query("SELECT uss FROM UserSleepSetting uss " +
           "JOIN FETCH uss.user u " +
           "WHERE u.isActive = true " +
           "ORDER BY uss.user.id")
    List<UserSleepSetting> findAllActiveUserSettings();

    /**
     * 사용자 ID로 설정 삭제
     */
    void deleteByUserId(Long userId);

    // === 동적 스케줄링을 위한 추가 쿼리 메서드들 ===

    /**
     * 특정 시간대에 수면 알림을 받아야 하는 사용자 설정 조회
     * 평일 기준으로 조회
     */
    @Query("SELECT uss FROM UserSleepSetting uss " +
           "JOIN FETCH uss.user u " +
           "WHERE uss.sleepReminderEnabled = true " +
           "AND u.isActive = true " +
           "AND u.notificationEnabled = true " +
           "AND TIME(u.preferredBedtime) = :reminderTime")
    List<UserSleepSetting> findUsersForWeekdayReminder(@Param("reminderTime") java.time.LocalTime reminderTime);

    /**
     * 특정 시간대에 주말 수면 알림을 받아야 하는 사용자 설정 조회
     */
    @Query("SELECT uss FROM UserSleepSetting uss " +
           "JOIN FETCH uss.user u " +
           "WHERE uss.sleepReminderEnabled = true " +
           "AND uss.weekendDifferentSchedule = true " +
           "AND u.isActive = true " +
           "AND u.notificationEnabled = true " +
           "AND TIME(uss.weekendBedtime) = :reminderTime")
    List<UserSleepSetting> findUsersForWeekendReminder(@Param("reminderTime") java.time.LocalTime reminderTime);

    /**
     * 기상 알림이 활성화된 사용자 설정 조회
     */
    @Query("SELECT uss FROM UserSleepSetting uss " +
           "JOIN FETCH uss.user u " +
           "WHERE uss.wakeupReminderEnabled = true " +
           "AND u.isActive = true " +
           "AND u.notificationEnabled = true")
    List<UserSleepSetting> findAllWithWakeupReminderEnabled();

    /**
     * 특정 시간대에 기상 알림을 받아야 하는 사용자 설정 조회 (평일)
     */
    @Query("SELECT uss FROM UserSleepSetting uss " +
           "JOIN FETCH uss.user u " +
           "WHERE uss.wakeupReminderEnabled = true " +
           "AND u.isActive = true " +
           "AND u.notificationEnabled = true " +
           "AND TIME(u.preferredWakeupTime) = :reminderTime")
    List<UserSleepSetting> findUsersForWeekdayWakeUpReminder(@Param("reminderTime") java.time.LocalTime reminderTime);

    /**
     * 특정 시간대에 주말 기상 알림을 받아야 하는 사용자 설정 조회
     */
    @Query("SELECT uss FROM UserSleepSetting uss " +
           "JOIN FETCH uss.user u " +
           "WHERE uss.wakeupReminderEnabled = true " +
           "AND uss.weekendDifferentSchedule = true " +
           "AND u.isActive = true " +
           "AND u.notificationEnabled = true " +
           "AND TIME(uss.weekendWakeupTime) = :reminderTime")
    List<UserSleepSetting> findUsersForWeekendWakeUpReminder(@Param("reminderTime") java.time.LocalTime reminderTime);

    /**
     * 패턴 분석을 위한 최근 활성 사용자 설정 조회
     * 스마트 타이밍 계산에 사용
     */
    @Query("SELECT uss FROM UserSleepSetting uss " +
           "JOIN FETCH uss.user u " +
           "WHERE uss.smartTimingEnabled = true " +
           "AND u.isActive = true " +
           "AND u.lastLoginAt >= :recentThreshold " +
           "ORDER BY u.lastLoginAt DESC")
    List<UserSleepSetting> findRecentActiveUsersForPatternAnalysis(@Param("recentThreshold") java.time.LocalDateTime recentThreshold);
}