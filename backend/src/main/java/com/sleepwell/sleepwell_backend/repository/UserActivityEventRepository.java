package com.sleepwell.sleepwell_backend.repository;

import com.sleepwell.sleepwell_backend.entity.UserActivityEvent;
import com.sleepwell.sleepwell_backend.enums.ActivityEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 활동 이벤트 Repository
 *
 * UserActivityEvent 엔티티에 대한 데이터베이스 접근을 제공합니다.
 * 사용자별, 이벤트 타입별, 기간별 이벤트 조회 기능을 제공합니다.
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Repository
public interface UserActivityEventRepository extends JpaRepository<UserActivityEvent, Long> {

    /**
     * 사용자 ID로 모든 이벤트 조회
     *
     * @param userId 사용자 ID
     * @return 해당 사용자의 모든 이벤트 리스트
     */
    List<UserActivityEvent> findByUserId(Long userId);

    /**
     * 사용자 ID와 이벤트 타입으로 이벤트 조회
     * 인덱스 활용: idx_user_event_type (user_id, event_type)
     *
     * @param userId 사용자 ID
     * @param eventType 이벤트 타입
     * @return 해당 사용자의 특정 타입 이벤트 리스트
     */
    List<UserActivityEvent> findByUserIdAndEventType(Long userId, ActivityEventType eventType);

    /**
     * 사용자 ID와 이벤트 타입으로 이벤트 조회 (페이징)
     *
     * @param userId 사용자 ID
     * @param eventType 이벤트 타입
     * @param pageable 페이지 정보
     * @return 페이징 처리된 이벤트
     */
    Page<UserActivityEvent> findByUserIdAndEventType(Long userId, ActivityEventType eventType, Pageable pageable);

    /**
     * 사용자 ID와 기간으로 이벤트 조회
     * 날짜 범위 검색 시 인덱스 활용: idx_created_at
     *
     * @param userId 사용자 ID
     * @param startDate 시작 일시
     * @param endDate 종료 일시
     * @return 기간 내 이벤트 리스트
     */
    @Query("SELECT e FROM UserActivityEvent e WHERE e.user.id = :userId " +
           "AND e.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY e.createdAt DESC")
    List<UserActivityEvent> findUserActivityInDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * 사용자 ID, 이벤트 타입, 기간으로 이벤트 조회
     *
     * @param userId 사용자 ID
     * @param eventType 이벤트 타입
     * @param startDate 시작 일시
     * @param endDate 종료 일시
     * @return 조건에 맞는 이벤트 리스트
     */
    @Query("SELECT e FROM UserActivityEvent e WHERE e.user.id = :userId " +
           "AND e.eventType = :eventType " +
           "AND e.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY e.createdAt DESC")
    List<UserActivityEvent> findUserActivityByTypeAndDateRange(
        @Param("userId") Long userId,
        @Param("eventType") ActivityEventType eventType,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * 세션 ID로 이벤트 조회
     * 동일 세션 내 이벤트 추적용
     * 인덱스 활용: idx_session_id
     *
     * @param sessionId 세션 ID
     * @return 해당 세션의 모든 이벤트 리스트
     */
    List<UserActivityEvent> findBySessionId(String sessionId);

    /**
     * 특정 이벤트 타입의 총 개수 조회 (통계용)
     *
     * @param eventType 이벤트 타입
     * @return 해당 타입 이벤트 총 개수
     */
    long countByEventType(ActivityEventType eventType);

    /**
     * 사용자별 이벤트 타입 카운트 조회
     *
     * @param userId 사용자 ID
     * @param eventType 이벤트 타입
     * @return 해당 사용자의 특정 타입 이벤트 개수
     */
    long countByUserIdAndEventType(Long userId, ActivityEventType eventType);

    /**
     * 최근 N일간 로그인한 고유 사용자 수 조회 (관리자 대시보드용)
     *
     * @param eventType 이벤트 타입 (LOGIN)
     * @param startDate 시작 일시
     * @return 고유 사용자 수
     */
    @Query("SELECT COUNT(DISTINCT e.user.id) FROM UserActivityEvent e " +
           "WHERE e.eventType = :eventType " +
           "AND e.createdAt >= :startDate")
    long countDistinctUsersByEventTypeAndDateAfter(
        @Param("eventType") ActivityEventType eventType,
        @Param("startDate") LocalDateTime startDate
    );

    /**
     * AI 기능별 사용 통계 조회 (관리자 대시보드용)
     * AI 관련 이벤트만 조회하여 카테고리별로 집계
     *
     * @param startDate 시작 일시
     * @param endDate 종료 일시
     * @return 카테고리별 이벤트 개수 (category, count)
     */
    @Query("SELECT e.eventCategory, COUNT(e) FROM UserActivityEvent e " +
           "WHERE e.eventType IN (" +
           "  com.sleepwell.sleepwell_backend.enums.ActivityEventType.AI_SLEEPY, " +
           "  com.sleepwell.sleepwell_backend.enums.ActivityEventType.AI_INSOMNIA, " +
           "  com.sleepwell.sleepwell_backend.enums.ActivityEventType.AI_SLEEP_TEST, " +
           "  com.sleepwell.sleepwell_backend.enums.ActivityEventType.AI_STRESS, " +
           "  com.sleepwell.sleepwell_backend.enums.ActivityEventType.AI_ACUPRESSURE" +
           ") " +
           "AND e.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY e.eventCategory")
    List<Object[]> getAIFeatureUsageStatistics(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * 특정 기간 내 가장 활발한 사용자 Top N 조회 (관리자 대시보드용)
     *
     * @param startDate 시작 일시
     * @param endDate 종료 일시
     * @param pageable 페이지 정보 (limit 설정용)
     * @return 사용자 ID와 이벤트 수 (userId, count)
     */
    @Query("SELECT e.user.id, COUNT(e) as eventCount FROM UserActivityEvent e " +
           "WHERE e.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY e.user.id " +
           "ORDER BY eventCount DESC")
    Page<Object[]> findMostActiveUsers(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
}
