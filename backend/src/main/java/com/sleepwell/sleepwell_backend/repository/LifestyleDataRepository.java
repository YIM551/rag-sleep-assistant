package com.sleepwell.sleepwell_backend.repository;

import com.sleepwell.sleepwell_backend.entity.LifestyleData;
import com.sleepwell.sleepwell_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 생활 패턴 데이터 Repository
 * 
 * 사용자의 일상 활동과 생활 패턴 데이터에 대한 데이터 액세스 레이어를 제공합니다.
 * ML 모델 학습을 위한 다양한 조회 메서드와 통계 분석 메서드를 포함합니다.
 * 
 * @author SleepWell Development Team
 * @since 1.0
 * @see LifestyleData
 */
@Repository
public interface LifestyleDataRepository extends JpaRepository<LifestyleData, Long> {

    // === 기본 조회 메서드 ===

    /**
     * 사용자별 특정 날짜의 생활 패턴 데이터 조회
     */
    Optional<LifestyleData> findByUserAndRecordDate(User user, LocalDate recordDate);

    /**
     * 사용자별 특정 기간 생활 패턴 데이터 조회 (날짜 범위)
     */
    List<LifestyleData> findByUserAndRecordDateBetweenOrderByRecordDateDesc(
            User user, LocalDate startDate, LocalDate endDate);

    /**
     * 사용자별 최근 N일간 생활 패턴 데이터 조회 (페이징)
     */
    Page<LifestyleData> findByUserAndRecordDateAfterOrderByRecordDateDesc(
            User user, LocalDate date, Pageable pageable);

    /**
     * 사용자별 최신 생활 패턴 데이터 조회
     */
    Optional<LifestyleData> findFirstByUserOrderByRecordDateDesc(User user);

    // === 운동 활동 분석 ===

    /**
     * 특정 기간 동안 운동한 날들의 생활 패턴 데이터 조회
     */
    @Query("SELECT ld FROM LifestyleData ld WHERE ld.user = :user " +
           "AND ld.recordDate BETWEEN :startDate AND :endDate " +
           "AND ld.exerciseType IS NOT NULL " +
           "ORDER BY ld.recordDate DESC")
    List<LifestyleData> findExerciseDaysByUserAndDateRange(
            @Param("user") User user, 
            @Param("startDate") LocalDate startDate, 
            @Param("endDate") LocalDate endDate);

    /**
     * 운동 유형별 데이터 조회 (특정 기간)
     */
    @Query("SELECT ld FROM LifestyleData ld WHERE ld.user = :user " +
           "AND ld.recordDate BETWEEN :startDate AND :endDate " +
           "AND ld.exerciseType = :exerciseType " +
           "ORDER BY ld.recordDate DESC")
    List<LifestyleData> findByUserAndDateRangeAndExerciseType(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("exerciseType") String exerciseType);

    /**
     * 고강도 운동 일자 조회
     */
    @Query("SELECT ld FROM LifestyleData ld WHERE ld.user = :user " +
           "AND ld.recordDate BETWEEN :startDate AND :endDate " +
           "AND ld.exerciseIntensity IN ('HIGH', 'INTENSE') " +
           "ORDER BY ld.recordDate DESC")
    List<LifestyleData> findHighIntensityExerciseDays(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // === 카페인 및 알코올 분석 ===

    /**
     * 카페인 섭취가 있는 날들 조회
     */
    @Query("SELECT ld FROM LifestyleData ld WHERE ld.user = :user " +
           "AND ld.recordDate BETWEEN :startDate AND :endDate " +
           "AND ld.caffeineIntake > 0 " +
           "ORDER BY ld.recordDate DESC")
    List<LifestyleData> findCaffeineDays(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 높은 카페인 섭취일 조회 (임계값 이상)
     */
    @Query("SELECT ld FROM LifestyleData ld WHERE ld.user = :user " +
           "AND ld.recordDate BETWEEN :startDate AND :endDate " +
           "AND ld.caffeineIntake >= :threshold " +
           "ORDER BY ld.recordDate DESC")
    List<LifestyleData> findHighCaffeineDays(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("threshold") Integer threshold);

    /**
     * 알코올 섭취가 있는 날들 조회
     */
    @Query("SELECT ld FROM LifestyleData ld WHERE ld.user = :user " +
           "AND ld.recordDate BETWEEN :startDate AND :endDate " +
           "AND ld.alcoholIntake > 0 " +
           "ORDER BY ld.recordDate DESC")
    List<LifestyleData> findAlcoholDays(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // === 스트레스 및 심리적 상태 분석 ===

    /**
     * 높은 스트레스 수준 일자 조회
     */
    @Query("SELECT ld FROM LifestyleData ld WHERE ld.user = :user " +
           "AND ld.recordDate BETWEEN :startDate AND :endDate " +
           "AND ld.stressLevel >= :threshold " +
           "ORDER BY ld.recordDate DESC")
    List<LifestyleData> findHighStressDays(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("threshold") Integer threshold);

    /**
     * 특별한 이벤트가 있었던 날들 조회
     */
    @Query("SELECT ld FROM LifestyleData ld WHERE ld.user = :user " +
           "AND ld.recordDate BETWEEN :startDate AND :endDate " +
           "AND ld.significantEvent = true " +
           "ORDER BY ld.recordDate DESC")
    List<LifestyleData> findSignificantEventDays(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // === 화면 노출 시간 분석 ===

    /**
     * 높은 화면 노출 시간 일자 조회
     */
    @Query("SELECT ld FROM LifestyleData ld WHERE ld.user = :user " +
           "AND ld.recordDate BETWEEN :startDate AND :endDate " +
           "AND ld.screenTimeMinutes >= :threshold " +
           "ORDER BY ld.recordDate DESC")
    List<LifestyleData> findHighScreenTimeDays(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("threshold") Integer threshold);

    /**
     * 취침 전 화면 노출이 높은 일자 조회
     */
    @Query("SELECT ld FROM LifestyleData ld WHERE ld.user = :user " +
           "AND ld.recordDate BETWEEN :startDate AND :endDate " +
           "AND ld.preBedrimeScreenTime >= :threshold " +
           "ORDER BY ld.recordDate DESC")
    List<LifestyleData> findHighPreBedtimeScreenDays(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("threshold") Integer threshold);

    // === 수면 루틴 분석 ===

    /**
     * 수면 루틴을 실행한 날들 조회
     */
    @Query("SELECT ld FROM LifestyleData ld WHERE ld.user = :user " +
           "AND ld.recordDate BETWEEN :startDate AND :endDate " +
           "AND ld.sleepRoutineFollowed = true " +
           "ORDER BY ld.recordDate DESC")
    List<LifestyleData> findSleepRoutineDays(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 특정 이완 활동을 한 날들 조회
     */
    @Query("SELECT ld FROM LifestyleData ld WHERE ld.user = :user " +
           "AND ld.recordDate BETWEEN :startDate AND :endDate " +
           "AND ld.relaxationActivity = :activity " +
           "ORDER BY ld.recordDate DESC")
    List<LifestyleData> findRelaxationActivityDays(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("activity") String activity);

    // === ML 분석용 통계 쿼리 ===

    /**
     * 사용자별 특정 기간 평균 운동 시간 계산
     */
    @Query("SELECT AVG(ld.exerciseDurationMinutes) FROM LifestyleData ld " +
           "WHERE ld.user = :user AND ld.recordDate BETWEEN :startDate AND :endDate " +
           "AND ld.exerciseDurationMinutes IS NOT NULL")
    Optional<Double> findAverageExerciseDuration(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 사용자별 특정 기간 평균 카페인 섭취량 계산
     */
    @Query("SELECT AVG(ld.caffeineIntake) FROM LifestyleData ld " +
           "WHERE ld.user = :user AND ld.recordDate BETWEEN :startDate AND :endDate")
    Optional<Double> findAverageCaffeineIntake(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 사용자별 특정 기간 평균 스트레스 수준 계산
     */
    @Query("SELECT AVG(ld.stressLevel) FROM LifestyleData ld " +
           "WHERE ld.user = :user AND ld.recordDate BETWEEN :startDate AND :endDate " +
           "AND ld.stressLevel IS NOT NULL")
    Optional<Double> findAverageStressLevel(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 사용자별 특정 기간 평균 화면 노출 시간 계산
     */
    @Query("SELECT AVG(ld.screenTimeMinutes) FROM LifestyleData ld " +
           "WHERE ld.user = :user AND ld.recordDate BETWEEN :startDate AND :endDate " +
           "AND ld.screenTimeMinutes IS NOT NULL")
    Optional<Double> findAverageScreenTime(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // === 패턴 분석용 쿼리 ===

    /**
     * 요일별 생활 패턴 데이터 조회 (ML 학습용)
     */
    @Query("SELECT ld FROM LifestyleData ld WHERE ld.user = :user " +
           "AND ld.recordDate BETWEEN :startDate AND :endDate " +
           "AND FUNCTION('DAYOFWEEK', ld.recordDate) = :dayOfWeek " +
           "ORDER BY ld.recordDate DESC")
    List<LifestyleData> findByUserAndDayOfWeek(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("dayOfWeek") Integer dayOfWeek);

    /**
     * 데이터 소스별 생활 패턴 데이터 조회
     */
    List<LifestyleData> findByUserAndDataSourceAndRecordDateBetween(
            User user, String dataSource, LocalDate startDate, LocalDate endDate);

    /**
     * 특정 기간 내 완전한 데이터를 가진 날들 조회 (필수 필드가 모두 채워진 데이터)
     */
    @Query("SELECT ld FROM LifestyleData ld WHERE ld.user = :user " +
           "AND ld.recordDate BETWEEN :startDate AND :endDate " +
           "AND ld.stressLevel IS NOT NULL " +
           "AND ld.screenTimeMinutes IS NOT NULL " +
           "ORDER BY ld.recordDate DESC")
    List<LifestyleData> findCompleteDataByUserAndDateRange(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
} 