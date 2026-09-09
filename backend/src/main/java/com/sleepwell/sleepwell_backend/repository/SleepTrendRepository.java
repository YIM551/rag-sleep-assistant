package com.sleepwell.sleepwell_backend.repository;

import com.sleepwell.sleepwell_backend.entity.SleepTrend;
import com.sleepwell.sleepwell_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 수면 트렌드 Repository
 * 
 * 사용자별 장기 수면 패턴 트렌드와 개인 기준선 데이터에 대한 데이터 액세스 레이어를 제공합니다.
 * ML 모델의 시계열 분석과 개인화 기준점 설정을 위한 다양한 조회 메서드와 통계 분석 메서드를 포함합니다.
 * 
 * @author SleepWell Development Team
 * @since 1.0
 * @see SleepTrend
 */
@Repository
public interface SleepTrendRepository extends JpaRepository<SleepTrend, Long> {

    // === 기본 조회 메서드 ===

    /**
     * 사용자별 특정 날짜의 수면 트렌드 조회
     */
    Optional<SleepTrend> findByUserAndTrendDate(User user, LocalDate trendDate);

    /**
     * 사용자별 특정 트렌드 유형의 최신 트렌드 조회
     */
    Optional<SleepTrend> findFirstByUserAndTrendTypeOrderByTrendDateDesc(User user, String trendType);

    /**
     * 사용자별 특정 기간 트렌드 조회 (날짜 범위)
     */
    List<SleepTrend> findByUserAndTrendDateBetweenOrderByTrendDateDesc(
            User user, LocalDate startDate, LocalDate endDate);

    /**
     * 사용자별 특정 트렌드 유형과 기간의 트렌드 조회
     */
    List<SleepTrend> findByUserAndTrendTypeAndTrendDateBetweenOrderByTrendDateDesc(
            User user, String trendType, LocalDate startDate, LocalDate endDate);

    /**
     * 사용자별 최근 N개 트렌드 조회 (페이징)
     */
    Page<SleepTrend> findByUserAndTrendDateAfterOrderByTrendDateDesc(
            User user, LocalDate date, Pageable pageable);

    /**
     * 사용자별 최신 트렌드 조회
     */
    Optional<SleepTrend> findFirstByUserOrderByTrendDateDesc(User user);

    // === 트렌드 유형별 조회 ===

    /**
     * 주간 트렌드 조회
     */
    List<SleepTrend> findByUserAndTrendTypeAndTrendDateAfterOrderByTrendDateDesc(
            User user, String trendType, LocalDate afterDate);

    /**
     * 특정 트렌드 유형의 모든 데이터 조회
     */
    List<SleepTrend> findByUserAndTrendTypeOrderByTrendDateDesc(User user, String trendType);

    /**
     * 월별 트렌드 조회 (최근 12개월)
     */
    @Query("SELECT st FROM SleepTrend st WHERE st.user = :user " +
           "AND st.trendType = 'MONTHLY' " +
           "AND st.trendDate >= :afterDate " +
           "ORDER BY st.trendDate DESC")
    List<SleepTrend> findMonthlyTrendsAfterDate(
            @Param("user") User user,
            @Param("afterDate") LocalDate afterDate);

    /**
     * 계절별 트렌드 조회 (최근 4계절)
     */
    @Query("SELECT st FROM SleepTrend st WHERE st.user = :user " +
           "AND st.trendType = 'SEASONAL' " +
           "AND st.trendDate >= :afterDate " +
           "ORDER BY st.trendDate DESC")
    List<SleepTrend> findSeasonalTrendsAfterDate(
            @Param("user") User user,
            @Param("afterDate") LocalDate afterDate);

    // === 트렌드 분석 결과 기반 조회 ===

    /**
     * 개선 추세 트렌드 조회
     */
    @Query("SELECT st FROM SleepTrend st WHERE st.user = :user " +
           "AND st.trendDate BETWEEN :startDate AND :endDate " +
           "AND st.trendDirection = 'IMPROVING' " +
           "ORDER BY st.trendDate DESC")
    List<SleepTrend> findImprovingTrends(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 악화 추세 트렌드 조회
     */
    @Query("SELECT st FROM SleepTrend st WHERE st.user = :user " +
           "AND st.trendDate BETWEEN :startDate AND :endDate " +
           "AND st.trendDirection = 'DECLINING' " +
           "ORDER BY st.trendDate DESC")
    List<SleepTrend> findDecliningTrends(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 높은 트렌드 점수 조회 (개선 추세)
     */
    @Query("SELECT st FROM SleepTrend st WHERE st.user = :user " +
           "AND st.trendDate BETWEEN :startDate AND :endDate " +
           "AND st.trendScore >= :threshold " +
           "ORDER BY st.trendScore DESC")
    List<SleepTrend> findHighTrendScores(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("threshold") Integer threshold);

    /**
     * 낮은 트렌드 점수 조회 (악화 추세)
     */
    @Query("SELECT st FROM SleepTrend st WHERE st.user = :user " +
           "AND st.trendDate BETWEEN :startDate AND :endDate " +
           "AND st.trendScore <= :threshold " +
           "ORDER BY st.trendScore ASC")
    List<SleepTrend> findLowTrendScores(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("threshold") Integer threshold);

    // === 신뢰도 및 품질 기반 조회 ===

    /**
     * 높은 신뢰도 트렌드 조회
     */
    @Query("SELECT st FROM SleepTrend st WHERE st.user = :user " +
           "AND st.trendDate BETWEEN :startDate AND :endDate " +
           "AND st.confidenceLevel = 'HIGH' " +
           "ORDER BY st.trendDate DESC")
    List<SleepTrend> findHighConfidenceTrends(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 충분한 데이터 포인트를 가진 트렌드 조회
     */
    @Query("SELECT st FROM SleepTrend st WHERE st.user = :user " +
           "AND st.trendDate BETWEEN :startDate AND :endDate " +
           "AND st.dataPointCount >= :minDataPoints " +
           "ORDER BY st.dataPointCount DESC")
    List<SleepTrend> findTrendsWithSufficientData(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("minDataPoints") Integer minDataPoints);

    /**
     * 최신 계산 트렌드 조회 (특정 시간 이후 계산된 것)
     */
    @Query("SELECT st FROM SleepTrend st WHERE st.user = :user " +
           "AND st.calculatedAt >= :afterDateTime " +
           "ORDER BY st.calculatedAt DESC")
    List<SleepTrend> findRecentlyCalculatedTrends(
            @Param("user") User user,
            @Param("afterDateTime") LocalDateTime afterDateTime);

    // === 이상치 및 주기성 분석 ===

    /**
     * 이상치가 많은 기간의 트렌드 조회
     */
    @Query("SELECT st FROM SleepTrend st WHERE st.user = :user " +
           "AND st.trendDate BETWEEN :startDate AND :endDate " +
           "AND st.anomalyCount >= :threshold " +
           "ORDER BY st.anomalyCount DESC")
    List<SleepTrend> findHighAnomalyTrends(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("threshold") Integer threshold);

    /**
     * 주기성이 감지된 트렌드 조회
     */
    @Query("SELECT st FROM SleepTrend st WHERE st.user = :user " +
           "AND st.trendDate BETWEEN :startDate AND :endDate " +
           "AND st.cyclicityDetected = true " +
           "ORDER BY st.trendDate DESC")
    List<SleepTrend> findCyclicTrends(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 규칙적인 패턴을 가진 트렌드 조회 (높은 일관성 점수)
     */
    @Query("SELECT st FROM SleepTrend st WHERE st.user = :user " +
           "AND st.trendDate BETWEEN :startDate AND :endDate " +
           "AND st.consistencyScore >= :threshold " +
           "ORDER BY st.consistencyScore DESC")
    List<SleepTrend> findConsistentTrends(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("threshold") Integer threshold);

    // === 환경 요인 영향 분석 ===

    /**
     * 계절적 영향을 많이 받는 트렌드 조회
     */
    @Query("SELECT st FROM SleepTrend st WHERE st.user = :user " +
           "AND st.trendDate BETWEEN :startDate AND :endDate " +
           "AND ABS(st.seasonalImpact) >= :threshold " +
           "ORDER BY ABS(st.seasonalImpact) DESC")
    List<SleepTrend> findSeasonallySensitiveTrends(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("threshold") Double threshold);

    /**
     * 날씨 영향을 많이 받는 트렌드 조회
     */
    @Query("SELECT st FROM SleepTrend st WHERE st.user = :user " +
           "AND st.trendDate BETWEEN :startDate AND :endDate " +
           "AND ABS(st.weatherImpact) >= :threshold " +
           "ORDER BY ABS(st.weatherImpact) DESC")
    List<SleepTrend> findWeatherSensitiveTrends(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("threshold") Double threshold);

    /**
     * 라이프스타일 영향을 많이 받는 트렌드 조회
     */
    @Query("SELECT st FROM SleepTrend st WHERE st.user = :user " +
           "AND st.trendDate BETWEEN :startDate AND :endDate " +
           "AND ABS(st.lifestyleImpact) >= :threshold " +
           "ORDER BY ABS(st.lifestyleImpact) DESC")
    List<SleepTrend> findLifestyleSensitiveTrends(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("threshold") Double threshold);

    // === 비교 분석 ===

    /**
     * 평균 대비 좋은 성과 트렌드 조회
     */
    @Query("SELECT st FROM SleepTrend st WHERE st.user = :user " +
           "AND st.trendDate BETWEEN :startDate AND :endDate " +
           "AND st.ageGroupPercentile >= :threshold " +
           "ORDER BY st.ageGroupPercentile DESC")
    List<SleepTrend> findAboveAverageTrends(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("threshold") Integer threshold);

    /**
     * 평균 대비 낮은 성과 트렌드 조회
     */
    @Query("SELECT st FROM SleepTrend st WHERE st.user = :user " +
           "AND st.trendDate BETWEEN :startDate AND :endDate " +
           "AND st.ageGroupPercentile <= :threshold " +
           "ORDER BY st.ageGroupPercentile ASC")
    List<SleepTrend> findBelowAverageTrends(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("threshold") Integer threshold);

    // === ML 분석용 통계 쿼리 ===

    /**
     * 사용자별 특정 기간 평균 수면 시간 계산 (트렌드 기반)
     */
    @Query("SELECT AVG(st.averageSleepMinutes) FROM SleepTrend st " +
           "WHERE st.user = :user AND st.trendDate BETWEEN :startDate AND :endDate " +
           "AND st.averageSleepMinutes IS NOT NULL")
    Optional<Double> findAverageSleepMinutesTrend(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 사용자별 특정 기간 평균 수면 효율성 계산 (트렌드 기반)
     */
    @Query("SELECT AVG(st.averageSleepEfficiency) FROM SleepTrend st " +
           "WHERE st.user = :user AND st.trendDate BETWEEN :startDate AND :endDate " +
           "AND st.averageSleepEfficiency IS NOT NULL")
    Optional<Double> findAverageSleepEfficiencyTrend(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 사용자별 특정 기간 평균 수면 점수 계산 (트렌드 기반)
     */
    @Query("SELECT AVG(st.averageSleepScore) FROM SleepTrend st " +
           "WHERE st.user = :user AND st.trendDate BETWEEN :startDate AND :endDate " +
           "AND st.averageSleepScore IS NOT NULL")
    Optional<Double> findAverageSleepScoreTrend(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 전체 트렌드 점수 분포 계산
     */
    @Query("SELECT AVG(st.trendScore), " +
           "MIN(st.trendScore), " +
           "MAX(st.trendScore) " +
           "FROM SleepTrend st " +
           "WHERE st.user = :user AND st.trendDate BETWEEN :startDate AND :endDate " +
           "AND st.trendScore IS NOT NULL")
    Object[] findTrendScoreStatistics(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // === 예측 분석 ===

    /**
     * 높은 예측 신뢰도를 가진 트렌드 조회
     */
    @Query("SELECT st FROM SleepTrend st WHERE st.user = :user " +
           "AND st.trendDate BETWEEN :startDate AND :endDate " +
           "AND st.predictionConfidence >= :threshold " +
           "ORDER BY st.predictionConfidence DESC")
    List<SleepTrend> findHighPredictionConfidenceTrends(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("threshold") Integer threshold);

    /**
     * 목표 달성 가능성이 높은 트렌드 조회
     */
    @Query("SELECT st FROM SleepTrend st WHERE st.user = :user " +
           "AND st.trendDate BETWEEN :startDate AND :endDate " +
           "AND st.goalAchievabilityPercent >= :threshold " +
           "ORDER BY st.goalAchievabilityPercent DESC")
    List<SleepTrend> findHighGoalAchievabilityTrends(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("threshold") Integer threshold);

    // === 업데이트 관리 ===

    /**
     * 업데이트가 필요한 트렌드 조회 (다음 업데이트 일정이 지난 것들)
     */
    @Query("SELECT st FROM SleepTrend st WHERE st.user = :user " +
           "AND st.nextUpdateDate <= :currentDate " +
           "ORDER BY st.nextUpdateDate ASC")
    List<SleepTrend> findTrendsNeedingUpdate(
            @Param("user") User user,
            @Param("currentDate") LocalDate currentDate);

    /**
     * 특정 분석 버전의 트렌드 조회
     */
    List<SleepTrend> findByUserAndAnalysisVersionOrderByTrendDateDesc(User user, String analysisVersion);

    /**
     * 특정 기간별 트렌드 조회 (기간 일수 기준)
     */
    List<SleepTrend> findByUserAndPeriodDaysAndTrendDateBetweenOrderByTrendDateDesc(
            User user, Integer periodDays, LocalDate startDate, LocalDate endDate);

    /**
     * 가장 최근 각 트렌드 유형별 트렌드 조회
     */
    @Query("SELECT st FROM SleepTrend st " +
           "WHERE st.user = :user " +
           "AND st.id IN (" +
           "    SELECT MAX(st2.id) FROM SleepTrend st2 " +
           "    WHERE st2.user = :user " +
           "    GROUP BY st2.trendType" +
           ") " +
           "ORDER BY st.trendDate DESC")
    List<SleepTrend> findLatestTrendsByType(@Param("user") User user);
} 