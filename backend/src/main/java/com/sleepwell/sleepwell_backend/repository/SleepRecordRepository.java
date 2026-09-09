package com.sleepwell.sleepwell_backend.repository;

import com.sleepwell.sleepwell_backend.dto.DisruptionStatsDto;
import com.sleepwell.sleepwell_backend.dto.EnvironmentStatsDto;
import com.sleepwell.sleepwell_backend.dto.SleepPatternStatsDto;
import com.sleepwell.sleepwell_backend.dto.WeeklySleepPatternDto;
import com.sleepwell.sleepwell_backend.entity.SleepRecord;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.enums.WearableSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 수면 기록 Repository
 * 수면 데이터 조회, 통계, 분석을 위한 쿼리 메서드들을 제공합니다.
 */
@Repository
public interface SleepRecordRepository extends JpaRepository<SleepRecord, Long> {

    @Query("SELECT sr FROM SleepRecord sr JOIN FETCH sr.user WHERE sr.user = :user ORDER BY sr.recordDate DESC")
    List<SleepRecord> findAllByUser(@Param("user") User user);

    @Query(value = "SELECT sr FROM SleepRecord sr JOIN FETCH sr.user WHERE sr.user = :user",
           countQuery = "SELECT count(sr) FROM SleepRecord sr WHERE sr.user = :user")
    Page<SleepRecord> findAllByUser(@Param("user") User user, Pageable pageable);

    @Query("SELECT sr FROM SleepRecord sr JOIN FETCH sr.user WHERE sr.user = :user AND sr.recordDate = :recordDate ORDER BY sr.createdAt DESC LIMIT 1")
    Optional<SleepRecord> findByUserAndDate(@Param("user") User user, @Param("recordDate") LocalDate recordDate);

    boolean existsByUserAndRecordDate(User user, LocalDate recordDate);

    @Query("SELECT sr FROM SleepRecord sr JOIN FETCH sr.user WHERE sr.user = :user AND sr.recordDate BETWEEN :startDate AND :endDate ORDER BY sr.recordDate DESC")
    List<SleepRecord> findByDateRange(@Param("user") User user, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query(value = "SELECT sr FROM SleepRecord sr JOIN FETCH sr.user WHERE sr.user = :user AND sr.recordDate BETWEEN :startDate AND :endDate ORDER BY sr.recordDate DESC",
           countQuery = "SELECT count(sr) FROM SleepRecord sr WHERE sr.user = :user AND sr.recordDate BETWEEN :startDate AND :endDate")
    Page<SleepRecord> findByDateRange(@Param("user") User user, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("SELECT sr FROM SleepRecord sr JOIN FETCH sr.user WHERE sr.user = :user AND sr.recordDate >= :fromDate ORDER BY sr.recordDate DESC")
    List<SleepRecord> findByFromDate(@Param("user") User user, @Param("fromDate") LocalDate fromDate);

    @Query("SELECT AVG(sr.totalSleepMinutes) FROM SleepRecord sr WHERE sr.user = :user")
    Optional<Double> findAverageSleepMinutes(@Param("user") User user);

    @Query("SELECT AVG(sr.totalSleepMinutes) FROM SleepRecord sr WHERE sr.user = :user AND sr.recordDate BETWEEN :startDate AND :endDate")
    Optional<Double> findAverageSleepMinutesByDateRange(@Param("user") User user,
                                        @Param("startDate") LocalDate startDate, 
                                        @Param("endDate") LocalDate endDate);

    @Query("SELECT AVG(sr.sleepQualityScore) FROM SleepRecord sr WHERE sr.user = :user AND sr.sleepQualityScore IS NOT NULL")
    Optional<Double> findAverageQualityScore(@Param("user") User user);

    @Query("SELECT sr FROM SleepRecord sr WHERE sr.user = :user AND sr.sleepQualityScore BETWEEN :minScore AND :maxScore ORDER BY sr.recordDate DESC")
    List<SleepRecord> findByQualityScoreRange(@Param("user") User user, @Param("minScore") Integer minScore, @Param("maxScore") Integer maxScore);

    @Query("SELECT sr FROM SleepRecord sr WHERE sr.user = :user AND sr.snoreDetected = true ORDER BY sr.recordDate DESC")
    List<SleepRecord> findWithSnore(@Param("user") User user);

    @Query("SELECT sr FROM SleepRecord sr WHERE sr.user = :user AND sr.bruxismDetected = true ORDER BY sr.recordDate DESC")
    List<SleepRecord> findWithBruxism(@Param("user") User user);

    @Query("SELECT sr FROM SleepRecord sr WHERE sr.user = :user AND sr.sleepTalkDetected = true ORDER BY sr.recordDate DESC")
    List<SleepRecord> findWithSleepTalk(@Param("user") User user);

    @Query("SELECT sr FROM SleepRecord sr WHERE sr.user = :user AND sr.wearableSource = :source ORDER BY sr.recordDate DESC")
    List<SleepRecord> findBySource(@Param("user") User user, @Param("source") WearableSource wearableSource);

    @Query("SELECT new com.sleepwell.sleepwell_backend.dto.SleepPatternStatsDto(" +
            "AVG(sr.totalSleepMinutes), " +
            "AVG(sr.deepSleepMinutes), " +
            "AVG(sr.lightSleepMinutes), " +
            "AVG(sr.remSleepMinutes), " +
            "AVG(sr.wakeupCount), " +
            "AVG(sr.sleepQualityScore)) " +
           "FROM SleepRecord sr WHERE sr.user = :user")
    Optional<SleepPatternStatsDto> findSleepPatternStats(@Param("user") User user);

    @Query("SELECT new com.sleepwell.sleepwell_backend.dto.EnvironmentStatsDto(" +
            "AVG(sr.lightLevel), " +
            "AVG(sr.noiseLevel), " +
            "AVG(sr.temperature), " +
            "AVG(sr.humidity)) " +
           "FROM SleepRecord sr WHERE sr.user = :user AND sr.recordDate BETWEEN :startDate AND :endDate")
    Optional<EnvironmentStatsDto> findEnvironmentStatsByDateRange(@Param("user") User user,
                                 @Param("startDate") LocalDate startDate, 
                                 @Param("endDate") LocalDate endDate);

    @Query("SELECT sr FROM SleepRecord sr WHERE sr.user = :user AND sr.sleepQualityScore = " +
           "(SELECT MAX(sr2.sleepQualityScore) FROM SleepRecord sr2 WHERE sr2.user = :user)")
    List<SleepRecord> findTopQualityRecords(@Param("user") User user);

    @Query("SELECT sr FROM SleepRecord sr WHERE sr.user = :user AND sr.sleepQualityScore = " +
           "(SELECT MIN(sr2.sleepQualityScore) FROM SleepRecord sr2 WHERE sr2.user = :user)")
    List<SleepRecord> findWorstQualityRecords(@Param("user") User user);

    @Query("SELECT new com.sleepwell.sleepwell_backend.dto.DisruptionStatsDto(" +
            "SUM(CASE WHEN sr.snoreDetected = true THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN sr.bruxismDetected = true THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN sr.sleepTalkDetected = true THEN 1 ELSE 0 END)) " +
           "FROM SleepRecord sr WHERE sr.user = :user AND sr.recordDate BETWEEN :startDate AND :endDate")
    Optional<DisruptionStatsDto> findDisruptionStatsByDateRange(@Param("user") User user,
                               @Param("startDate") LocalDate startDate, 
                               @Param("endDate") LocalDate endDate);

    // ==================== 오디오 이벤트 통계용 메서드 ====================

    /**
     * 사용자별 기간별 수면 기록 조회 (오디오 이벤트 통계용)
     */
    @Query("SELECT sr FROM SleepRecord sr WHERE sr.user.id = :userId " +
            "AND sr.recordDate BETWEEN :startDate AND :endDate " +
            "ORDER BY sr.recordDate DESC")
    List<SleepRecord> findByUserIdAndRecordDateBetween(@Param("userId") Long userId,
                                                        @Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate);
    
    @Query("SELECT sr FROM SleepRecord sr WHERE sr.user = :user ORDER BY sr.recordDate DESC, sr.createdAt DESC LIMIT 1")
    Optional<SleepRecord> findLatestByUser(@Param("user") User user);

    Optional<SleepRecord> findByUserAndRecordDate(User user, LocalDate recordDate);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM SleepRecord sr WHERE sr.user = :user")
    void deleteAllByUser(@Param("user") User user);
    
    /**
     * 사용자의 가장 최근 수면 기록 조회
     */
    @Query("SELECT sr FROM SleepRecord sr WHERE sr.user.id = :userId ORDER BY sr.createdAt DESC")
    Page<SleepRecord> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
    
    default Optional<SleepRecord> findFirstByUserIdOrderByCreatedAtDesc(Long userId) {
        Page<SleepRecord> page = findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 1));
        return page.hasContent() ? Optional.of(page.getContent().get(0)) : Optional.empty();
    }
    
    // Admin 기능을 위한 추가 메서드들
    
    /**
     * 기간별 수면 기록 수 조회
     */
    @Query("SELECT COUNT(sr) FROM SleepRecord sr WHERE sr.createdAt BETWEEN :start AND :end")
    Long countByCreatedAtBetween(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);
    
    /**
     * 전체 평균 수면 품질
     */
    @Query("SELECT AVG(sr.sleepQualityScore) FROM SleepRecord sr WHERE sr.sleepQualityScore IS NOT NULL")
    Double findAverageSleepQuality();
    
    /**
     * 전체 평균 수면 시간
     */
    @Query("SELECT AVG(sr.totalSleepMinutes) FROM SleepRecord sr")
    Double findAverageSleepDuration();
    
    /**
     * 웨어러블 소스별 기록 수
     */
    @Query("SELECT sr.wearableSource, COUNT(sr) FROM SleepRecord sr WHERE sr.wearableSource IS NOT NULL GROUP BY sr.wearableSource")
    List<Object[]> countByWearableSource();
    
    /**
     * 최근 수면 기록 조회
     */
    List<SleepRecord> findTop10ByOrderByCreatedAtDesc();
    
    /**
     * 사용자별 기록 수
     */
    @Query("SELECT COUNT(sr) FROM SleepRecord sr WHERE sr.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);
    
    /**
     * 최근 사용자 수면 기록
     */
    @Query("SELECT sr FROM SleepRecord sr WHERE sr.user.id = :userId ORDER BY sr.createdAt DESC")
    List<SleepRecord> findTop5ByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * 사용자별 평균 수면 시간
     */
    @Query("SELECT AVG(sr.totalSleepMinutes) FROM SleepRecord sr WHERE sr.user.id = :userId")
    Double findAverageSleepDurationByUserId(@Param("userId") Long userId);
    
    /**
     * 사용자별 평균 수면 품질
     */
    @Query("SELECT AVG(sr.sleepQualityScore) FROM SleepRecord sr WHERE sr.user.id = :userId AND sr.sleepQualityScore IS NOT NULL")
    Double findAverageSleepQualityByUserId(@Param("userId") Long userId);
    
    /**
     * 기간별 평균 수면 시간
     */
    @Query("SELECT AVG(sr.totalSleepMinutes) FROM SleepRecord sr WHERE sr.createdAt BETWEEN :start AND :end")
    Double findAverageSleepDurationBetween(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);
    
    /**
     * 기간별 평균 수면 품질
     */
    @Query("SELECT AVG(sr.sleepQualityScore) FROM SleepRecord sr WHERE sr.sleepQualityScore IS NOT NULL AND sr.createdAt BETWEEN :start AND :end")
    Double findAverageSleepQualityBetween(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);
    
    /**
     * 고유 사용자 수
     */
    @Query("SELECT COUNT(DISTINCT sr.user.id) FROM SleepRecord sr")
    Long countUniqueUsers();
    
    // Admin 기능을 위한 추가 메서드들
    @Query("SELECT AVG(sr.sleepQualityScore) FROM SleepRecord sr WHERE sr.user.id = :userId AND sr.recordDate >= :startDate")
    Double findRecentAverageSleepQuality(@Param("userId") Long userId, @Param("startDate") LocalDate startDate);
    
    @Query("SELECT AVG(sr.totalSleepMinutes) FROM SleepRecord sr WHERE sr.user.id = :userId AND sr.recordDate >= :startDate")
    Double findRecentAverageSleepDuration(@Param("userId") Long userId, @Param("startDate") LocalDate startDate);
    
    @Query("SELECT AVG(sr.sleepQualityScore) FROM SleepRecord sr WHERE sr.createdAt BETWEEN :startDate AND :endDate")
    Double findAverageQualityBetween(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);
    
    @Query("SELECT AVG(sr.totalSleepMinutes) FROM SleepRecord sr WHERE sr.createdAt BETWEEN :startDate AND :endDate")
    Double findAverageDurationBetween(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);
    
    /**
     * 사용자의 특정 기간 내 수면 기록 조회 (시작 시간 기준 정렬)
     */
    @Query("SELECT sr FROM SleepRecord sr WHERE sr.user = :user " +
           "AND sr.sleepStartTime BETWEEN :startTime AND :endTime " +
           "ORDER BY sr.sleepStartTime DESC")
    List<SleepRecord> findByUserAndSleepStartTimeBetweenOrderBySleepStartTimeDesc(
            @Param("user") User user, 
            @Param("startTime") java.time.LocalDateTime startTime, 
            @Param("endTime") java.time.LocalDateTime endTime);

    // === 스마트 타이밍 계산을 위한 추가 메서드들 ===

    /**
     * 사용자의 특정 날짜 이후 수면 기록 조회 (생성일 기준 역순 정렬)
     * 스마트 타이밍 계산에 사용
     */
    @Query("SELECT sr FROM SleepRecord sr WHERE sr.user.id = :userId " +
           "AND sr.createdAt >= :since " +
           "ORDER BY sr.createdAt DESC")
    List<SleepRecord> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("since") java.time.LocalDateTime since);
} 