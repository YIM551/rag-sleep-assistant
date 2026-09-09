package com.sleepwell.sleepwell_backend.repository;

import com.sleepwell.sleepwell_backend.entity.ConsultationSession;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.enums.ConsultationTopic;
import com.sleepwell.sleepwell_backend.enums.SessionStatus;
import com.sleepwell.sleepwell_backend.enums.SessionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 음성 상담 세션 Repository
 * 실시간 상담 세션 관리 및 WebSocket 지원을 위한 최적화된 쿼리 제공
 */
@Repository
public interface ConsultationSessionRepository extends JpaRepository<ConsultationSession, Long> {

    /**
     * 사용자의 모든 상담 세션 조회 (최신순)
     */
    @Query("SELECT cs FROM ConsultationSession cs WHERE cs.user = :user ORDER BY cs.consultationTime DESC")
    Page<ConsultationSession> findByUser(@Param("user") User user, Pageable pageable);

    /**
     * 사용자의 활성 상담 세션 조회 (진행 중 또는 예약된 세션)
     */
    @Query("SELECT cs FROM ConsultationSession cs WHERE cs.user = :user " +
           "AND cs.status IN ('SCHEDULED', 'IN_PROGRESS') ORDER BY cs.consultationTime ASC")
    List<ConsultationSession> findActiveByUser(@Param("user") User user);

    /**
     * 특정 상태의 상담 세션 조회
     */
    @Query("SELECT cs FROM ConsultationSession cs WHERE cs.status = :status ORDER BY cs.consultationTime ASC")
    List<ConsultationSession> findByStatus(@Param("status") SessionStatus status);

    /**
     * 상담 주제별 세션 조회
     */
    @Query("SELECT cs FROM ConsultationSession cs WHERE cs.topic = :topic " +
           "AND cs.status = :status ORDER BY cs.consultationTime DESC")
    Page<ConsultationSession> findByTopicAndStatus(@Param("topic") ConsultationTopic topic, 
                                                   @Param("status") SessionStatus status, 
                                                   Pageable pageable);

    /**
     * 세션 유형별 조회
     */
    @Query("SELECT cs FROM ConsultationSession cs WHERE cs.sessionType = :sessionType " +
           "ORDER BY cs.consultationTime DESC")
    Page<ConsultationSession> findBySessionType(@Param("sessionType") SessionType sessionType, Pageable pageable);

    /**
     * 예약된 상담 세션 조회 (특정 시간 범위)
     */
    @Query("SELECT cs FROM ConsultationSession cs WHERE cs.status = 'SCHEDULED' " +
           "AND cs.consultationTime BETWEEN :startTime AND :endTime ORDER BY cs.consultationTime ASC")
    List<ConsultationSession> findScheduledSessions(@Param("startTime") LocalDateTime startTime,
                                                    @Param("endTime") LocalDateTime endTime);

    /**
     * 진행 중인 상담 세션 조회 (실시간 모니터링용)
     */
    @Query("SELECT cs FROM ConsultationSession cs WHERE cs.status = 'IN_PROGRESS' " +
           "ORDER BY cs.consultationTime ASC")
    List<ConsultationSession> findInProgressSessions();

    /**
     * 사용자의 최근 완료된 상담 세션 조회
     */
    @Query("SELECT cs FROM ConsultationSession cs WHERE cs.user = :user " +
           "AND cs.status = 'COMPLETED' ORDER BY cs.endTime DESC")
    Page<ConsultationSession> findRecentCompletedByUser(@Param("user") User user, Pageable pageable);

    /**
     * 특정 기간 내 생성된 상담 세션 조회
     */
    @Query("SELECT cs FROM ConsultationSession cs WHERE cs.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY cs.createdAt DESC")
    List<ConsultationSession> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    /**
     * 상담 주제별 통계 (완료된 세션 기준)
     */
    @Query("SELECT cs.topic, COUNT(cs) FROM ConsultationSession cs " +
           "WHERE cs.status = 'COMPLETED' GROUP BY cs.topic ORDER BY COUNT(cs) DESC")
    List<Object[]> getTopicStatistics();

    /**
     * 세션 유형별 통계
     */
    @Query("SELECT cs.sessionType, COUNT(cs), AVG(cs.userSatisfactionScore) " +
           "FROM ConsultationSession cs WHERE cs.status = 'COMPLETED' " +
           "GROUP BY cs.sessionType ORDER BY COUNT(cs) DESC")
    List<Object[]> getSessionTypeStatistics();

    /**
     * 사용자별 상담 세션 개수 조회
     */
    @Query("SELECT COUNT(cs) FROM ConsultationSession cs WHERE cs.user = :user")
    Long countByUser(@Param("user") User user);

    /**
     * 사용자별 완료된 상담 세션 개수
     */
    @Query("SELECT COUNT(cs) FROM ConsultationSession cs WHERE cs.user = :user AND cs.status = 'COMPLETED'")
    Long countCompletedByUser(@Param("user") User user);

    /**
     * 평균 상담 시간 계산 (완료된 세션 기준)
     */
    @Query("SELECT AVG(cs.totalDurationMinutes) FROM ConsultationSession cs " +
           "WHERE cs.status = 'COMPLETED' AND cs.totalDurationMinutes IS NOT NULL")
    Double getAverageSessionDuration();

    /**
     * 사용자 만족도 평균 점수
     */
    @Query("SELECT AVG(cs.userSatisfactionScore) FROM ConsultationSession cs " +
           "WHERE cs.status = 'COMPLETED' AND cs.userSatisfactionScore IS NOT NULL")
    Double getAverageUserSatisfaction();

    /**
     * AI 품질 평균 점수
     */
    @Query("SELECT AVG(cs.aiQualityScore) FROM ConsultationSession cs " +
           "WHERE cs.status = 'COMPLETED' AND cs.aiQualityScore IS NOT NULL")
    Double getAverageAiQuality();

    /**
     * 높은 만족도 세션 조회 (만족도 4점 이상)
     */
    @Query("SELECT cs FROM ConsultationSession cs WHERE cs.userSatisfactionScore >= 4 " +
           "AND cs.status = 'COMPLETED' ORDER BY cs.userSatisfactionScore DESC, cs.endTime DESC")
    Page<ConsultationSession> findHighSatisfactionSessions(Pageable pageable);

    /**
     * 응급 상담 세션 조회 (우선순위 높음)
     */
    @Query("SELECT cs FROM ConsultationSession cs WHERE cs.sessionType = 'EMERGENCY' " +
           "ORDER BY cs.consultationTime ASC")
    List<ConsultationSession> findEmergencySessions();

    /**
     * 사용자의 최신 상담 세션 조회
     */
    @Query("SELECT cs FROM ConsultationSession cs WHERE cs.user = :user " +
           "ORDER BY cs.consultationTime DESC LIMIT 1")
    Optional<ConsultationSession> findLatestByUser(@Param("user") User user);

    /**
     * 월별 상담 세션 통계
     */
    @Query("SELECT FUNCTION('YEAR', cs.consultationTime), FUNCTION('MONTH', cs.consultationTime), " +
           "COUNT(cs), AVG(cs.userSatisfactionScore) " +
           "FROM ConsultationSession cs WHERE cs.status = 'COMPLETED' " +
           "GROUP BY FUNCTION('YEAR', cs.consultationTime), FUNCTION('MONTH', cs.consultationTime) " +
           "ORDER BY FUNCTION('YEAR', cs.consultationTime) DESC, FUNCTION('MONTH', cs.consultationTime) DESC")
    List<Object[]> getMonthlyStatistics();

    /**
     * 장기 세션 조회 (30분 이상)
     */
    @Query("SELECT cs FROM ConsultationSession cs WHERE cs.totalDurationMinutes >= 30 " +
           "AND cs.status = 'COMPLETED' ORDER BY cs.totalDurationMinutes DESC")
    Page<ConsultationSession> findLongSessions(Pageable pageable);

    /**
     * 사용자 ID로 세션 조회 (최신순)
     */
    @Query("SELECT cs FROM ConsultationSession cs WHERE cs.user.id = :userId ORDER BY cs.consultationTime DESC")
    Page<ConsultationSession> findByUserIdOrderByStartTimeDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * 사용자와 상태로 세션 조회
     */
    Optional<ConsultationSession> findByUserAndStatus(User user, SessionStatus status);

    /**
     * 세션 ID와 사용자로 세션 조회
     */
    Optional<ConsultationSession> findByIdAndUser(Long id, User user);

    /**
     * 사용자의 모든 상담 세션 조회 (최신순) - 페이징
     */
    Page<ConsultationSession> findByUserOrderByConsultationTimeDesc(User user, Pageable pageable);

    /**
     * 사용자와 상태로 세션 존재 여부 확인
     */
    boolean existsByUserAndStatus(User user, SessionStatus status);

    /**
     * 사용자별 특정 기간 내 세션 조회
     */
    @Query("SELECT cs FROM ConsultationSession cs WHERE cs.user = :user " +
           "AND cs.consultationTime BETWEEN :startDate AND :endDate " +
           "ORDER BY cs.consultationTime DESC")
    List<ConsultationSession> findByUserAndDateRange(
            @Param("user") User user,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 진행 중인 세션 수 조회
     */
    @Query("SELECT COUNT(cs) FROM ConsultationSession cs WHERE cs.status = :status")
    long countByStatus(@Param("status") SessionStatus status);

    /**
     * 특정 기간 내 완료된 세션 조회 (통계용)
     */
    @Query("SELECT cs FROM ConsultationSession cs WHERE cs.status = 'COMPLETED' " +
           "AND cs.endTime BETWEEN :startDate AND :endDate")
    List<ConsultationSession> findCompletedSessionsBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    // Admin 기능을 위한 추가 메서드들
    
    /**
     * 기간별 상담 수 조회
     */
    @Query("SELECT COUNT(cs) FROM ConsultationSession cs WHERE cs.createdAt BETWEEN :start AND :end")
    Long countByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 사용자별 상담 수
     */
    @Query("SELECT COUNT(cs) FROM ConsultationSession cs WHERE cs.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);
    
    /**
     * 필터링된 상담 검색
     */
    @Query("SELECT cs FROM ConsultationSession cs WHERE "
         + "(:userId IS NULL OR cs.user.id = :userId) "
         + "AND (:topic IS NULL OR cs.topic = :topic) "
         + "AND (:startDate IS NULL OR cs.createdAt >= :startDate) "
         + "AND (:endDate IS NULL OR cs.createdAt <= :endDate)")
    Page<ConsultationSession> findWithFilters(
        @Param("userId") Long userId,
        @Param("topic") String topic,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    /**
     * 최근 사용자 상담 세션
     */
    @Query("SELECT cs FROM ConsultationSession cs WHERE cs.user.id = :userId ORDER BY cs.createdAt DESC")
    List<ConsultationSession> findTop5ByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * 고유 사용자 수 조회
     */
    @Query("SELECT COUNT(DISTINCT cs.user.id) FROM ConsultationSession cs WHERE cs.createdAt BETWEEN :start AND :end")
    Long countUniqueUsersBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 주제별 분포
     */
    @Query("SELECT cs.topic, COUNT(cs) FROM ConsultationSession cs "
         + "WHERE cs.createdAt BETWEEN :start AND :end "
         + "GROUP BY cs.topic")
    List<Object[]> getTopicDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * AI 모델별 사용 통계 (aiModel 필드가 없으므로 기본값 사용)
     */
    @Query("SELECT 'gpt-4', COUNT(cs), SUM(CASE WHEN cs.totalDurationMinutes IS NOT NULL THEN cs.totalDurationMinutes * 10 ELSE 0 END) FROM ConsultationSession cs "
         + "WHERE cs.createdAt BETWEEN :start AND :end")
    List<Object[]> getModelUsageStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 고유 사용자 수
     */
    @Query("SELECT COUNT(DISTINCT cs.user.id) FROM ConsultationSession cs")
    Long countUniqueUsers();

    /**
     * 사용자 ID와 세션 타입으로 세션 목록 조회 (페이징)
     */
    @Query("SELECT cs FROM ConsultationSession cs WHERE cs.user.id = :userId AND cs.sessionType = :sessionType ORDER BY cs.createdAt DESC")
    Page<ConsultationSession> findByUserIdAndSessionTypeOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("sessionType") com.sleepwell.sleepwell_backend.enums.SessionType sessionType,
            Pageable pageable);
} 