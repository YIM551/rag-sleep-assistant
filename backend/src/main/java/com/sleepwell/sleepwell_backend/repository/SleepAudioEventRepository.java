package com.sleepwell.sleepwell_backend.repository;

import com.sleepwell.sleepwell_backend.entity.SleepAudioEvent;
import com.sleepwell.sleepwell_backend.enums.AudioEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 수면 오디오 이벤트 Repository
 * 
 * SleepAudioEvent 엔티티에 대한 데이터 액세스 계층을 제공합니다.
 * 통계 계산과 트렌드 분석을 위한 효율적인 쿼리 메서드들을 포함합니다.
 * 
 * 주요 기능:
 * - 사용자별, 기간별 오디오 이벤트 조회
 * - 이벤트 유형별 필터링
 * - 강도 및 빈도 기반 검색
 * - 통계 계산을 위한 집계 쿼리
 * 
 * @author SleepWell Development Team
 * @since 1.0
 * @see SleepAudioEvent
 * @see AudioEventType
 */
@Repository
public interface SleepAudioEventRepository extends JpaRepository<SleepAudioEvent, Long> {

    // ==================== 기본 조회 메서드 ====================

    /**
     * 사용자별 오디오 이벤트 조회
     */
    List<SleepAudioEvent> findByUserIdOrderByEventStartTimeDesc(Long userId);

    /**
     * 수면 기록별 오디오 이벤트 조회
     */
    List<SleepAudioEvent> findBySleepRecordIdOrderByEventStartTimeDesc(Long sleepRecordId);

    /**
     * 사용자 및 수면 기록별 오디오 이벤트 조회
     */
    List<SleepAudioEvent> findByUserIdAndSleepRecordIdOrderByEventStartTimeDesc(Long userId, Long sleepRecordId);

    /**
     * 특정 오디오 이벤트 조회 (사용자 권한 확인 포함)
     */
    Optional<SleepAudioEvent> findByIdAndUserId(Long eventId, Long userId);

    // ==================== 기간별 조회 메서드 ====================

    /**
     * 사용자별 기간별 오디오 이벤트 조회
     */
    @Query("SELECT ae FROM SleepAudioEvent ae " +
           "WHERE ae.user.id = :userId " +
           "AND ae.eventDate BETWEEN :startDate AND :endDate " +
           "ORDER BY ae.eventStartTime DESC")
    List<SleepAudioEvent> findByUserIdAndEventDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 수면 기록별 기간별 오디오 이벤트 조회
     */
    @Query("SELECT ae FROM SleepAudioEvent ae " +
           "WHERE ae.sleepRecord.id = :sleepRecordId " +
           "AND ae.eventDate BETWEEN :startDate AND :endDate " +
           "ORDER BY ae.eventStartTime DESC")
    List<SleepAudioEvent> findBySleepRecordIdAndEventDateBetween(
            @Param("sleepRecordId") Long sleepRecordId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // ==================== 이벤트 유형별 조회 메서드 ====================

    /**
     * 사용자별 이벤트 유형별 조회
     */
    List<SleepAudioEvent> findByUserIdAndEventTypeOrderByEventStartTimeDesc(Long userId, AudioEventType eventType);

    /**
     * 사용자별 기간별 이벤트 유형별 조회
     */
    @Query("SELECT ae FROM SleepAudioEvent ae " +
           "WHERE ae.user.id = :userId " +
           "AND ae.eventType = :eventType " +
           "AND ae.eventDate BETWEEN :startDate AND :endDate " +
           "ORDER BY ae.eventStartTime DESC")
    List<SleepAudioEvent> findByUserIdAndEventTypeAndEventDateBetween(
            @Param("userId") Long userId,
            @Param("eventType") AudioEventType eventType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // ==================== 강도 기반 조회 메서드 ====================

    /**
     * 고강도 이벤트 조회 (강도 7 이상)
     */
    @Query("SELECT ae FROM SleepAudioEvent ae " +
           "WHERE ae.user.id = :userId " +
           "AND ae.intensityLevel >= 7 " +
           "AND ae.eventDate BETWEEN :startDate AND :endDate " +
           "ORDER BY ae.intensityLevel DESC, ae.eventStartTime DESC")
    List<SleepAudioEvent> findHighIntensityEventsByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 의료진 상담 필요 이벤트 조회
     */
    @Query("SELECT ae FROM SleepAudioEvent ae " +
           "WHERE ae.user.id = :userId " +
           "AND ((ae.eventType = 'SNORING' AND ae.intensityLevel >= 8) " +
           "     OR (ae.eventType = 'BRUXISM' AND ae.intensityLevel >= 7)) " +
           "AND ae.eventDate BETWEEN :startDate AND :endDate " +
           "ORDER BY ae.intensityLevel DESC, ae.eventStartTime DESC")
    List<SleepAudioEvent> findMedicalAttentionRequiredEvents(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // ==================== 통계 계산 메서드 ====================

    /**
     * 사용자별 기간별 총 이벤트 수 조회
     */
    @Query("SELECT COUNT(ae) FROM SleepAudioEvent ae " +
           "WHERE ae.user.id = :userId " +
           "AND ae.eventDate BETWEEN :startDate AND :endDate")
    Long countByUserIdAndEventDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 사용자별 기간별 이벤트 유형별 개수 조회
     */
    @Query("SELECT ae.eventType, COUNT(ae) FROM SleepAudioEvent ae " +
           "WHERE ae.user.id = :userId " +
           "AND ae.eventDate BETWEEN :startDate AND :endDate " +
           "GROUP BY ae.eventType")
    List<Object[]> countByEventTypeAndUserIdAndEventDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 사용자별 기간별 평균 강도 조회
     */
    @Query("SELECT AVG(ae.intensityLevel) FROM SleepAudioEvent ae " +
           "WHERE ae.user.id = :userId " +
           "AND ae.eventDate BETWEEN :startDate AND :endDate " +
           "AND ae.intensityLevel IS NOT NULL")
    Double findAverageIntensityByUserIdAndEventDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 사용자별 기간별 이벤트 유형별 평균 강도 조회
     */
    @Query("SELECT ae.eventType, AVG(ae.intensityLevel) FROM SleepAudioEvent ae " +
           "WHERE ae.user.id = :userId " +
           "AND ae.eventDate BETWEEN :startDate AND :endDate " +
           "AND ae.intensityLevel IS NOT NULL " +
           "GROUP BY ae.eventType")
    List<Object[]> findAverageIntensityByEventTypeAndUserIdAndEventDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 사용자별 기간별 평균 지속시간 조회
     */
    @Query("SELECT AVG(ae.durationSeconds) FROM SleepAudioEvent ae " +
           "WHERE ae.user.id = :userId " +
           "AND ae.eventDate BETWEEN :startDate AND :endDate " +
           "AND ae.durationSeconds IS NOT NULL")
    Double findAverageDurationByUserIdAndEventDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 사용자별 기간별 고강도 이벤트 비율 조회
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN ae.intensityLevel >= 7 THEN 1 END) * 100.0 / COUNT(ae) " +
           "FROM SleepAudioEvent ae " +
           "WHERE ae.user.id = :userId " +
           "AND ae.eventDate BETWEEN :startDate AND :endDate " +
           "AND ae.intensityLevel IS NOT NULL")
    Double findHighIntensityRatioByUserIdAndEventDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // ==================== 트렌드 분석 메서드 ====================

    /**
     * 일별 이벤트 수 트렌드 조회
     */
    @Query("SELECT DATE(ae.eventDate), COUNT(ae) FROM SleepAudioEvent ae " +
           "WHERE ae.user.id = :userId " +
           "AND ae.eventDate BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(ae.eventDate) " +
           "ORDER BY DATE(ae.eventDate)")
    List<Object[]> findDailyEventCountTrend(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 일별 평균 강도 트렌드 조회
     */
    @Query("SELECT DATE(ae.eventDate), AVG(ae.intensityLevel) FROM SleepAudioEvent ae " +
           "WHERE ae.user.id = :userId " +
           "AND ae.eventDate BETWEEN :startDate AND :endDate " +
           "AND ae.intensityLevel IS NOT NULL " +
           "GROUP BY DATE(ae.eventDate) " +
           "ORDER BY DATE(ae.eventDate)")
    List<Object[]> findDailyIntensityTrend(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 시간대별 이벤트 분포 조회
     */
    @Query("SELECT HOUR(ae.eventStartTime), COUNT(ae) FROM SleepAudioEvent ae " +
           "WHERE ae.user.id = :userId " +
           "AND ae.eventDate BETWEEN :startDate AND :endDate " +
           "GROUP BY HOUR(ae.eventStartTime) " +
           "ORDER BY HOUR(ae.eventStartTime)")
    List<Object[]> findHourlyEventDistribution(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 강도별 이벤트 분포 조회
     */
    @Query("SELECT ae.intensityLevel, COUNT(ae) FROM SleepAudioEvent ae " +
           "WHERE ae.user.id = :userId " +
           "AND ae.eventDate BETWEEN :startDate AND :endDate " +
           "AND ae.intensityLevel IS NOT NULL " +
           "GROUP BY ae.intensityLevel " +
           "ORDER BY ae.intensityLevel")
    List<Object[]> findIntensityDistribution(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // ==================== 특화 분석 메서드 ====================

    /**
     * 코골이 특화 통계 조회
     */
    @Query("SELECT " +
           "COUNT(DISTINCT DATE(ae.eventDate)), " +
           "AVG(ae.intensityLevel), " +
           "MAX(ae.intensityLevel), " +
           "AVG(ae.frequencyPerHour), " +
           "COUNT(CASE WHEN ae.intensityLevel >= 8 THEN 1 END) * 100.0 / COUNT(ae) " +
           "FROM SleepAudioEvent ae " +
           "WHERE ae.user.id = :userId " +
           "AND ae.eventType = 'SNORING' " +
           "AND ae.eventDate BETWEEN :startDate AND :endDate")
    List<Object[]> findSnoringStatistics(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 이갈이 특화 통계 조회
     */
    @Query("SELECT " +
           "COUNT(DISTINCT DATE(ae.eventDate)), " +
           "AVG(ae.intensityLevel), " +
           "MAX(ae.intensityLevel), " +
           "AVG(ae.frequencyPerHour), " +
           "COUNT(CASE WHEN ae.intensityLevel >= 7 THEN 1 END) * 100.0 / COUNT(ae) " +
           "FROM SleepAudioEvent ae " +
           "WHERE ae.user.id = :userId " +
           "AND ae.eventType = 'BRUXISM' " +
           "AND ae.eventDate BETWEEN :startDate AND :endDate")
    List<Object[]> findBruxismStatistics(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 환경소음 특화 통계 조회
     */
    @Query("SELECT " +
           "COUNT(DISTINCT DATE(ae.eventDate)), " +
           "AVG(ae.decibelLevel), " +
           "MAX(ae.decibelLevel), " +
           "COUNT(CASE WHEN ae.decibelLevel >= 70 THEN 1 END) * 100.0 / COUNT(ae) " +
           "FROM SleepAudioEvent ae " +
           "WHERE ae.user.id = :userId " +
           "AND ae.eventType = 'ENVIRONMENTAL_NOISE' " +
           "AND ae.eventDate BETWEEN :startDate AND :endDate " +
           "AND ae.decibelLevel IS NOT NULL")
    List<Object[]> findEnvironmentalNoiseStatistics(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // ==================== 데이터 관리 메서드 ====================

    /**
     * 사용자별 오래된 오디오 이벤트 삭제 (데이터 정리용)
     */
    @Query("DELETE FROM SleepAudioEvent ae " +
           "WHERE ae.user.id = :userId " +
           "AND ae.eventDate < :cutoffDate")
    void deleteOldEventsByUserId(
            @Param("userId") Long userId,
            @Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 특정 수면 기록의 모든 오디오 이벤트 삭제
     */
    void deleteBySleepRecordId(Long sleepRecordId);

    /**
     * 사용자의 모든 오디오 이벤트 삭제
     */
    void deleteByUserId(Long userId);

    // ==================== 존재 여부 확인 메서드 ====================

    /**
     * 사용자의 특정 기간 내 오디오 이벤트 존재 여부 확인
     */
    @Query("SELECT COUNT(ae) > 0 FROM SleepAudioEvent ae " +
           "WHERE ae.user.id = :userId " +
           "AND ae.eventDate BETWEEN :startDate AND :endDate")
    boolean existsByUserIdAndEventDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 수면 기록에 오디오 이벤트 존재 여부 확인
     */
    boolean existsBySleepRecordId(Long sleepRecordId);

    /**
     * 사용자의 특정 유형 오디오 이벤트 존재 여부 확인
     */
    boolean existsByUserIdAndEventType(Long userId, AudioEventType eventType);
} 