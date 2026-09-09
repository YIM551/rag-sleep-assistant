package com.sleepwell.sleepwell_backend.repository;

import com.sleepwell.sleepwell_backend.entity.SleepFeedback;
import com.sleepwell.sleepwell_backend.entity.SleepRecord;
import com.sleepwell.sleepwell_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 수면 피드백 Repository
 * 
 * 사용자의 주관적인 수면 경험과 만족도 데이터에 대한 데이터 액세스 레이어를 제공합니다.
 * ML 모델 검증과 개인화 분석을 위한 다양한 조회 메서드와 통계 분석 메서드를 포함합니다.
 * 
 * @author SleepWell Development Team
 * @since 1.0
 * @see SleepFeedback
 */
@Repository
public interface SleepFeedbackRepository extends JpaRepository<SleepFeedback, Long> {

    // === 기본 조회 메서드 ===

    /**
     * 사용자별 특정 날짜의 수면 피드백 조회
     */
    Optional<SleepFeedback> findByUserAndFeedbackDate(User user, LocalDate feedbackDate);

    /**
     * 수면 기록에 대한 피드백 조회
     */
    Optional<SleepFeedback> findBySleepRecord(SleepRecord sleepRecord);

    /**
     * 사용자별 특정 기간 수면 피드백 조회 (날짜 범위)
     */
    List<SleepFeedback> findByUserAndFeedbackDateBetweenOrderByFeedbackDateDesc(
            User user, LocalDate startDate, LocalDate endDate);

    /**
     * 사용자별 최근 N일간 수면 피드백 조회 (페이징)
     */
    Page<SleepFeedback> findByUserAndFeedbackDateAfterOrderByFeedbackDateDesc(
            User user, LocalDate date, Pageable pageable);

    /**
     * 사용자별 최신 수면 피드백 조회
     */
    Optional<SleepFeedback> findFirstByUserOrderByFeedbackDateDesc(User user);

    // === 만족도 기반 분석 ===

    /**
     * 높은 만족도 피드백 조회
     */
    @Query("SELECT sf FROM SleepFeedback sf WHERE sf.user = :user " +
           "AND sf.feedbackDate BETWEEN :startDate AND :endDate " +
           "AND sf.overallSatisfaction >= :threshold " +
           "ORDER BY sf.feedbackDate DESC")
    List<SleepFeedback> findHighSatisfactionFeedbacks(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("threshold") Integer threshold);

    /**
     * 낮은 만족도 피드백 조회
     */
    @Query("SELECT sf FROM SleepFeedback sf WHERE sf.user = :user " +
           "AND sf.feedbackDate BETWEEN :startDate AND :endDate " +
           "AND sf.overallSatisfaction <= :threshold " +
           "ORDER BY sf.feedbackDate DESC")
    List<SleepFeedback> findLowSatisfactionFeedbacks(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("threshold") Integer threshold);

    /**
     * 좋은 수면으로 분류된 피드백 조회
     */
    @Query("SELECT sf FROM SleepFeedback sf WHERE sf.user = :user " +
           "AND sf.feedbackDate BETWEEN :startDate AND :endDate " +
           "AND sf.overallSatisfaction >= 4 " +
           "AND sf.fatigueRecovery >= 4 " +
           "AND sf.morningFreshness >= 4 " +
           "ORDER BY sf.feedbackDate DESC")
    List<SleepFeedback> findGoodSleepFeedbacks(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 수면 문제가 있는 피드백 조회
     */
    @Query("SELECT sf FROM SleepFeedback sf WHERE sf.user = :user " +
           "AND sf.feedbackDate BETWEEN :startDate AND :endDate " +
           "AND (sf.overallSatisfaction <= 2 OR sf.fatigueRecovery <= 2 " +
           "OR sf.sleepOnsetDifficulty >= 4 OR sf.daytimeSleepiness >= 4) " +
           "ORDER BY sf.feedbackDate DESC")
    List<SleepFeedback> findProblematicSleepFeedbacks(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // === 수면 과정 평가 분석 ===

    /**
     * 잠들기 어려움이 있는 피드백 조회
     */
    @Query("SELECT sf FROM SleepFeedback sf WHERE sf.user = :user " +
           "AND sf.feedbackDate BETWEEN :startDate AND :endDate " +
           "AND sf.sleepOnsetDifficulty >= :threshold " +
           "ORDER BY sf.feedbackDate DESC")
    List<SleepFeedback> findSleepOnsetDifficultyFeedbacks(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("threshold") Integer threshold);

    /**
     * 중간 각성이 많은 피드백 조회
     */
    @Query("SELECT sf FROM SleepFeedback sf WHERE sf.user = :user " +
           "AND sf.feedbackDate BETWEEN :startDate AND :endDate " +
           "AND sf.perceivedWakeupCount >= :threshold " +
           "ORDER BY sf.feedbackDate DESC")
    List<SleepFeedback> findHighWakeupCountFeedbacks(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("threshold") Integer threshold);

    // === 환경적 문제 분석 ===

    /**
     * 환경적 문제가 있는 피드백 조회
     */
    @Query("SELECT sf FROM SleepFeedback sf WHERE sf.user = :user " +
           "AND sf.feedbackDate BETWEEN :startDate AND :endDate " +
           "AND (sf.temperatureComfort <= 2 OR sf.noiseComfort <= 2 OR sf.lightingComfort <= 2) " +
           "ORDER BY sf.feedbackDate DESC")
    List<SleepFeedback> findEnvironmentalIssueFeedbacks(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 특정 방해 요소가 있는 피드백 조회
     */
    @Query("SELECT sf FROM SleepFeedback sf WHERE sf.user = :user " +
           "AND sf.feedbackDate BETWEEN :startDate AND :endDate " +
           "AND (sf.perceivedSnoring = true OR sf.partnerDisturbance = true " +
           "OR sf.externalNoiseDisturbance = true OR sf.stressDisturbance = true " +
           "OR sf.physicalDiscomfort = true) " +
           "ORDER BY sf.feedbackDate DESC")
    List<SleepFeedback> findDisruptedSleepFeedbacks(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // === 피드백 품질 분석 ===

    /**
     * 즉시 수집된 높은 품질 피드백 조회
     */
    @Query("SELECT sf FROM SleepFeedback sf WHERE sf.user = :user " +
           "AND sf.feedbackDate BETWEEN :startDate AND :endDate " +
           "AND sf.feedbackCollectedAt < FUNCTION('TIMESTAMPADD', HOUR, 4, sf.sleepRecord.sleepEndTime) " +
           "AND sf.completionPercentage >= :completionThreshold " +
           "ORDER BY sf.feedbackDate DESC")
    List<SleepFeedback> findHighQualityImmediateFeedbacks(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("completionThreshold") Integer completionThreshold);

    /**
     * 특정 수집 방법으로 수집된 피드백 조회
     */
    List<SleepFeedback> findByUserAndCollectionMethodAndFeedbackDateBetween(
            User user, String collectionMethod, LocalDate startDate, LocalDate endDate);

    /**
     * 신뢰도가 높은 피드백 조회
     */
    @Query("SELECT sf FROM SleepFeedback sf WHERE sf.user = :user " +
           "AND sf.feedbackDate BETWEEN :startDate AND :endDate " +
           "AND sf.reliabilityScore >= :threshold " +
           "ORDER BY sf.feedbackDate DESC")
    List<SleepFeedback> findHighReliabilityFeedbacks(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("threshold") Integer threshold);

    // === ML 분석용 통계 쿼리 ===

    /**
     * 사용자별 특정 기간 평균 전체 만족도 계산
     */
    @Query("SELECT AVG(sf.overallSatisfaction) FROM SleepFeedback sf " +
           "WHERE sf.user = :user AND sf.feedbackDate BETWEEN :startDate AND :endDate")
    Optional<Double> findAverageOverallSatisfaction(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 사용자별 특정 기간 평균 피로 회복도 계산
     */
    @Query("SELECT AVG(sf.fatigueRecovery) FROM SleepFeedback sf " +
           "WHERE sf.user = :user AND sf.feedbackDate BETWEEN :startDate AND :endDate")
    Optional<Double> findAverageFatigueRecovery(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 사용자별 특정 기간 평균 기상 후 컨디션 계산
     */
    @Query("SELECT AVG(sf.morningCondition) FROM SleepFeedback sf " +
           "WHERE sf.user = :user AND sf.feedbackDate BETWEEN :startDate AND :endDate")
    Optional<Double> findAverageMorningCondition(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 사용자별 특정 기간 평균 낮 시간 졸림 정도 계산
     */
    @Query("SELECT AVG(sf.daytimeSleepiness) FROM SleepFeedback sf " +
           "WHERE sf.user = :user AND sf.feedbackDate BETWEEN :startDate AND :endDate " +
           "AND sf.daytimeSleepiness IS NOT NULL")
    Optional<Double> findAverageDaytimeSleepiness(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // === 객관적 데이터와 주관적 피드백 매칭 분석 ===

    /**
     * 수면 기록과 함께 조회 (객관적 데이터와 주관적 피드백 비교 분석용)
     */
    @Query("SELECT sf FROM SleepFeedback sf " +
           "JOIN FETCH sf.sleepRecord sr " +
           "WHERE sf.user = :user " +
           "AND sf.feedbackDate BETWEEN :startDate AND :endDate " +
           "ORDER BY sf.feedbackDate DESC")
    List<SleepFeedback> findWithSleepRecordByUserAndDateRange(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 객관적 수면 효율성과 주관적 만족도 불일치 분석
     * 수면 효율성 = (실제 수면 시간 / 침대에 있던 시간) * 100
     */
    @Query("SELECT sf FROM SleepFeedback sf " +
           "JOIN sf.sleepRecord sr " +
           "WHERE sf.user = :user " +
           "AND sf.feedbackDate BETWEEN :startDate AND :endDate " +
           "AND sr.sleepInBedMinutes > 0 " +
           "AND (((sr.totalSleepMinutes * 100.0 / sr.sleepInBedMinutes) >= 85 AND sf.overallSatisfaction <= 2) " +
           "OR ((sr.totalSleepMinutes * 100.0 / sr.sleepInBedMinutes) <= 70 AND sf.overallSatisfaction >= 4)) " +
           "ORDER BY sf.feedbackDate DESC")
    List<SleepFeedback> findObjectiveSubjectiveMismatchFeedbacks(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // === 패턴 분석용 쿼리 ===

    /**
     * 요일별 피드백 조회 (ML 학습용)
     */
    @Query("SELECT sf FROM SleepFeedback sf WHERE sf.user = :user " +
           "AND sf.feedbackDate BETWEEN :startDate AND :endDate " +
           "AND FUNCTION('DAYOFWEEK', sf.feedbackDate) = :dayOfWeek " +
           "ORDER BY sf.feedbackDate DESC")
    List<SleepFeedback> findByUserAndDayOfWeek(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("dayOfWeek") Integer dayOfWeek);

    /**
     * 특정 기간 내 완전한 피드백 데이터 조회 (필수 필드가 모두 채워진 데이터)
     */
    @Query("SELECT sf FROM SleepFeedback sf WHERE sf.user = :user " +
           "AND sf.feedbackDate BETWEEN :startDate AND :endDate " +
           "AND sf.overallSatisfaction IS NOT NULL " +
           "AND sf.fatigueRecovery IS NOT NULL " +
           "AND sf.morningFreshness IS NOT NULL " +
           "AND sf.morningCondition IS NOT NULL " +
           "ORDER BY sf.feedbackDate DESC")
    List<SleepFeedback> findCompleteFeedbackByUserAndDateRange(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 특정 기간 내 피드백 제공률 계산 (수면 기록 대비 피드백 비율)
     */
    @Query("SELECT COUNT(sf) FROM SleepFeedback sf " +
           "WHERE sf.user = :user " +
           "AND sf.feedbackDate BETWEEN :startDate AND :endDate")
    Long countFeedbacksByUserAndDateRange(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 늦게 수집된 피드백 조회 (수면 후 12시간 이후 수집)
     */
    @Query("SELECT sf FROM SleepFeedback sf " +
           "WHERE sf.user = :user " +
           "AND sf.feedbackDate BETWEEN :startDate AND :endDate " +
           "AND sf.feedbackCollectedAt >= FUNCTION('TIMESTAMPADD', HOUR, 12, sf.sleepRecord.sleepEndTime) " +
           "ORDER BY sf.feedbackDate DESC")
    List<SleepFeedback> findDelayedFeedbacks(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM SleepFeedback sf WHERE sf.user = :user")
    void deleteAllByUser(@Param("user") User user);
} 