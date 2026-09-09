package com.sleepwell.sleepwell_backend.repository;

import com.sleepwell.sleepwell_backend.entity.SleepAnalysis;
import com.sleepwell.sleepwell_backend.entity.SleepRecord;
import com.sleepwell.sleepwell_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 수면 분석 Repository
 * AI 기반 수면 분석 결과 조회 및 트렌드 분석을 위한 쿼리 메서드들을 제공합니다.
 */
@Repository
public interface SleepAnalysisRepository extends JpaRepository<SleepAnalysis, Long> {

    /**
     * 사용자별 수면 분석 조회 (최신순)
     */
    List<SleepAnalysis> findByUserOrderByAnalysisDateDesc(User user);

    /**
     * 사용자별 수면 분석 페이징 조회 (최신순)
     */
    Page<SleepAnalysis> findByUserOrderByAnalysisDateDesc(User user, Pageable pageable);

    /**
     * 특정 수면 기록의 분석 결과 조회
     */
    Optional<SleepAnalysis> findBySleepRecord(SleepRecord sleepRecord);

    /**
     * 사용자의 특정 날짜 분석 결과 조회
     */
    Optional<SleepAnalysis> findByUserAndAnalysisDate(User user, LocalDate analysisDate);

    /**
     * 사용자의 특정 기간 분석 결과 조회
     */
    List<SleepAnalysis> findByUserAndAnalysisDateBetweenOrderByAnalysisDateDesc(
            User user, LocalDate startDate, LocalDate endDate);

    /**
     * 사용자의 최근 N일 분석 결과 조회
     */
    @Query("SELECT sa FROM SleepAnalysis sa WHERE sa.user = :user AND sa.analysisDate >= :fromDate ORDER BY sa.analysisDate DESC")
    List<SleepAnalysis> findRecentAnalyses(@Param("user") User user, @Param("fromDate") LocalDate fromDate);

    /**
     * 사용자의 평균 수면 점수 조회
     */
    @Query("SELECT AVG(sa.sleepScore) FROM SleepAnalysis sa WHERE sa.user = :user")
    Double getAverageSleepScore(@Param("user") User user);

    /**
     * 사용자의 특정 기간 평균 수면 점수 조회
     */
    @Query("SELECT AVG(sa.sleepScore) FROM SleepAnalysis sa WHERE sa.user = :user AND sa.analysisDate BETWEEN :startDate AND :endDate")
    Double getAverageSleepScoreBetween(@Param("user") User user, 
                                      @Param("startDate") LocalDate startDate, 
                                      @Param("endDate") LocalDate endDate);

    /**
     * 사용자의 평균 수면 효율성 조회
     */
    @Query("SELECT AVG(sa.sleepEfficiency) FROM SleepAnalysis sa WHERE sa.user = :user")
    Double getAverageSleepEfficiency(@Param("user") User user);

    /**
     * 수면 점수별 분석 결과 조회
     */
    List<SleepAnalysis> findByUserAndSleepScoreBetweenOrderByAnalysisDateDesc(
            User user, Integer minScore, Integer maxScore);

    /**
     * 특정 신뢰도 이상의 분석 결과 조회
     */
    List<SleepAnalysis> findByUserAndConfidenceScoreGreaterThanEqualOrderByAnalysisDateDesc(
            User user, BigDecimal minConfidence);

    /**
     * 사용자의 최고/최저 수면 점수 분석 조회
     */
    @Query("SELECT sa FROM SleepAnalysis sa WHERE sa.user = :user AND sa.sleepScore = " +
           "(SELECT MAX(sa2.sleepScore) FROM SleepAnalysis sa2 WHERE sa2.user = :user)")
    List<SleepAnalysis> findBestSleepScoreAnalyses(@Param("user") User user);

    @Query("SELECT sa FROM SleepAnalysis sa WHERE sa.user = :user AND sa.sleepScore = " +
           "(SELECT MIN(sa2.sleepScore) FROM SleepAnalysis sa2 WHERE sa2.user = :user)")
    List<SleepAnalysis> findWorstSleepScoreAnalyses(@Param("user") User user);

    /**
     * 모델 버전별 분석 결과 조회
     */
    List<SleepAnalysis> findByUserAndModelVersionOrderByAnalysisDateDesc(User user, String modelVersion);

    /**
     * 사용자의 수면 점수 트렌드 조회 (날짜별)
     */
    @Query("SELECT sa.analysisDate, sa.sleepScore, sa.sleepEfficiency FROM SleepAnalysis sa " +
           "WHERE sa.user = :user AND sa.analysisDate BETWEEN :startDate AND :endDate " +
           "ORDER BY sa.analysisDate ASC")
    List<Object[]> getSleepScoreTrend(@Param("user") User user, 
                                     @Param("startDate") LocalDate startDate, 
                                     @Param("endDate") LocalDate endDate);

    /**
     * 월별 수면 분석 통계
     */
    @Query("SELECT " +
           "YEAR(sa.analysisDate) as year, " +
           "MONTH(sa.analysisDate) as month, " +
           "AVG(sa.sleepScore) as avgScore, " +
           "AVG(sa.sleepEfficiency) as avgEfficiency, " +
           "COUNT(sa) as recordCount " +
           "FROM SleepAnalysis sa WHERE sa.user = :user " +
           "GROUP BY YEAR(sa.analysisDate), MONTH(sa.analysisDate) " +
           "ORDER BY YEAR(sa.analysisDate) DESC, MONTH(sa.analysisDate) DESC")
    List<Object[]> getMonthlySleepStatistics(@Param("user") User user);

    /**
     * 주별 수면 점수 패턴 조회 (월요일=1, 일요일=7)
     */
    @Query("SELECT DAYOFWEEK(sa.analysisDate) as dayOfWeek, " +
           "AVG(sa.sleepScore) as avgScore, " +
           "AVG(sa.sleepEfficiency) as avgEfficiency " +
           "FROM SleepAnalysis sa WHERE sa.user = :user " +
           "GROUP BY DAYOFWEEK(sa.analysisDate) " +
           "ORDER BY DAYOFWEEK(sa.analysisDate)")
    List<Object[]> getWeeklySleepScorePattern(@Param("user") User user);

    /**
     * 특정 기간 내 수면 개선 추세 조회
     */
    @Query("SELECT sa FROM SleepAnalysis sa WHERE sa.user = :user AND sa.analysisDate BETWEEN :startDate AND :endDate " +
           "ORDER BY sa.sleepScore DESC, sa.sleepEfficiency DESC")
    List<SleepAnalysis> findImprovementTrend(@Param("user") User user, 
                                           @Param("startDate") LocalDate startDate, 
                                           @Param("endDate") LocalDate endDate);

    /**
     * 사용자의 분석 결과 개수 조회
     */
    long countByUser(User user);

    /**
     * 특정 기간 내 분석 결과 개수 조회
     */
    long countByUserAndAnalysisDateBetween(User user, LocalDate startDate, LocalDate endDate);

    /**
     * 전체 시스템 평균 수면 점수 조회 (비교 참고용)
     */
    @Query("SELECT AVG(sa.sleepScore) FROM SleepAnalysis sa")
    Double getSystemAverageSleepScore();

    /**
     * 특정 점수 이상의 사용자 수 조회 (순위 계산용)
     */
    @Query("SELECT COUNT(DISTINCT sa.user) FROM SleepAnalysis sa WHERE sa.sleepScore >= :minScore")
    long countUsersWithScoreAbove(@Param("minScore") Integer minScore);

    /**
     * 주간/월간 트렌드가 존재하는 분석 결과 조회
     */
    List<SleepAnalysis> findByUserAndWeeklyTrendsIsNotNull(User user);
    List<SleepAnalysis> findByUserAndMonthlyTrendsIsNotNull(User user);
} 