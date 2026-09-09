package com.sleepwell.sleepwell_backend.repository;

import com.sleepwell.sleepwell_backend.entity.ASMRPlaySession;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.enums.ASMRSessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ASMR 재생 세션 Repository
 */
public interface ASMRPlaySessionRepository extends JpaRepository<ASMRPlaySession, Long> {

    /**
     * 사용자의 현재 활성 세션 조회
     */
    @Query("SELECT s FROM ASMRPlaySession s WHERE s.user.id = :userId AND s.status IN ('ACTIVE', 'PAUSED', 'FADING_OUT') ORDER BY s.startedAt DESC")
    Optional<ASMRPlaySession> findActiveSessionByUserId(@Param("userId") Long userId);

    /**
     * 사용자의 모든 활성 세션 조회 (다중 세션 지원)
     */
    @Query("SELECT s FROM ASMRPlaySession s WHERE s.user.id = :userId AND s.status IN ('ACTIVE', 'PAUSED', 'FADING_OUT') ORDER BY s.startedAt DESC")
    List<ASMRPlaySession> findAllActiveSessionsByUserId(@Param("userId") Long userId);

    /**
     * 사용자의 재생 히스토리 조회
     */
    Page<ASMRPlaySession> findByUserOrderByStartedAtDesc(User user, Pageable pageable);

    /**
     * 특정 콘텐츠의 사용자 재생 히스토리
     */
    @Query("SELECT s FROM ASMRPlaySession s WHERE s.user.id = :userId AND s.asmrContent.id = :contentId ORDER BY s.startedAt DESC")
    List<ASMRPlaySession> findByUserAndContent(@Param("userId") Long userId, @Param("contentId") Long contentId);

    /**
     * 타이머가 만료된 세션 조회 (배치 처리용)
     */
    @Query("SELECT s FROM ASMRPlaySession s WHERE s.timerExpiresAt <= :currentTime AND s.status IN ('ACTIVE', 'PAUSED') ORDER BY s.timerExpiresAt")
    List<ASMRPlaySession> findExpiredTimerSessions(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 페이드아웃 시작이 필요한 세션 조회
     */
    @Query("SELECT s FROM ASMRPlaySession s WHERE s.timerExpiresAt <= :fadeOutTime AND s.timerExpiresAt > :currentTime " +
           "AND s.status = 'ACTIVE' AND s.fadeOutEnabled = true ORDER BY s.timerExpiresAt")
    List<ASMRPlaySession> findSessionsNeedingFadeOut(@Param("currentTime") LocalDateTime currentTime,
                                                    @Param("fadeOutTime") LocalDateTime fadeOutTime);

    /**
     * 특정 시간 이후에 시작된 세션 수 조회 (통계용)
     */
    @Query("SELECT COUNT(s) FROM ASMRPlaySession s WHERE s.startedAt >= :startTime")
    long countSessionsSince(@Param("startTime") LocalDateTime startTime);

    /**
     * 특정 콘텐츠의 총 재생 횟수 조회
     */
    @Query("SELECT COUNT(s) FROM ASMRPlaySession s WHERE s.asmrContent.id = :contentId")
    long countByContentId(@Param("contentId") Long contentId);

    /**
     * 사용자의 총 재생 시간 조회 (분 단위)
     */
    @Query("SELECT COALESCE(SUM(s.totalPlayedSeconds), 0) / 60 FROM ASMRPlaySession s WHERE s.user.id = :userId")
    long getTotalPlayedMinutesByUserId(@Param("userId") Long userId);

    /**
     * 타이머 사용 통계 - 가장 많이 사용되는 타이머 시간
     */
    @Query("SELECT s.timerMinutes, COUNT(s) as count FROM ASMRPlaySession s WHERE s.timerMinutes IS NOT NULL " +
           "GROUP BY s.timerMinutes ORDER BY count DESC")
    List<Object[]> getTimerUsageStatistics();

    /**
     * 기간별 세션 통계
     */
    @Query("SELECT DATE(s.startedAt) as sessionDate, COUNT(s) as sessionCount, " +
           "AVG(s.totalPlayedSeconds) as avgPlayTime FROM ASMRPlaySession s " +
           "WHERE s.startedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(s.startedAt) ORDER BY sessionDate DESC")
    List<Object[]> getSessionStatistics(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    /**
     * 사용자별 선호 타이머 시간 조회
     */
    @Query("SELECT s.timerMinutes, COUNT(s) as usageCount FROM ASMRPlaySession s " +
           "WHERE s.user.id = :userId AND s.timerMinutes IS NOT NULL " +
           "GROUP BY s.timerMinutes ORDER BY usageCount DESC")
    List<Object[]> getUserTimerPreferences(@Param("userId") Long userId);

    /**
     * 고아 세션 정리 (24시간 이상 된 ACTIVE 상태 세션)
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ASMRPlaySession s SET s.status = 'ERROR', s.endedAt = :currentTime " +
           "WHERE s.status IN ('ACTIVE', 'PAUSED') AND s.startedAt < :cutoffTime")
    int cleanupOrphanSessions(@Param("currentTime") LocalDateTime currentTime,
                             @Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 오래된 종료 세션 물리적 삭제 (30일 이상)
     * STOPPED, TIMER_EXPIRED, ERROR 상태의 세션만 삭제
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ASMRPlaySession s " +
           "WHERE s.status IN ('STOPPED', 'TIMER_EXPIRED', 'ERROR') " +
           "AND s.endedAt < :cutoffTime")
    int deleteOldCompletedSessions(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 사용자의 최근 세션 조회 (완료된 세션만)
     */
    @Query("SELECT s FROM ASMRPlaySession s WHERE s.user.id = :userId " +
           "AND s.status IN ('STOPPED', 'TIMER_EXPIRED') ORDER BY s.endedAt DESC")
    List<ASMRPlaySession> findRecentCompletedSessions(@Param("userId") Long userId, Pageable pageable);

    /**
     * 평균 재생 시간이 긴 콘텐츠 조회 (추천용)
     */
    @Query("SELECT s.asmrContent.id, AVG(s.totalPlayedSeconds) as avgPlayTime FROM ASMRPlaySession s " +
           "WHERE s.totalPlayedSeconds > 0 GROUP BY s.asmrContent.id " +
           "HAVING COUNT(s) >= :minSessions ORDER BY avgPlayTime DESC")
    List<Object[]> findContentWithLongestPlayTime(@Param("minSessions") int minSessions);
}