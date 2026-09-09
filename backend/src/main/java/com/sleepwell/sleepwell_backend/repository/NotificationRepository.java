package com.sleepwell.sleepwell_backend.repository;

import com.sleepwell.sleepwell_backend.entity.Notification;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.enums.NotificationType;
import com.sleepwell.sleepwell_backend.enums.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 알림 Repository
 * 알림 조회, 발송 관리, 읽음 상태 관리 등을 위한 쿼리 메서드들을 제공합니다.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 특정 사용자의 알림을 최신순으로 조회합니다.
     *
     * @param user     사용자
     * @param pageable 페이징 정보
     * @return 페이징된 알림 목록
     */
    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * 특정 사용자 ID의 알림을 최신순으로 조회합니다.
     *
     * @param userId   사용자 ID
     * @param pageable 페이징 정보
     * @return 페이징된 알림 목록
     */
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    Page<Notification> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * 특정 사용자의 읽지 않은 알림 개수를 조회합니다.
     *
     * @param user 사용자
     * @param read 읽음 상태
     * @return 읽지 않은 알림 개수
     */
    long countByUserAndIsRead(User user, boolean read);

    /**
     * 특정 사용자 ID의 읽지 않은 알림 개수를 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 읽지 않은 알림 개수
     */
    long countByUserIdAndIsReadFalse(Long userId);

    /**
     * 특정 알림 ID와 사용자 ID로 알림을 조회합니다.
     *
     * @param notificationId 알림 ID
     * @param userId         사용자 ID
     * @return 알림 (Optional)
     */
    @Query("SELECT n FROM Notification n WHERE n.id = :notificationId AND n.user.id = :userId")
    Optional<Notification> findByIdAndUserId(@Param("notificationId") Long notificationId, @Param("userId") Long userId);

    /**
     * 발송 시간이 된 예약 알림들을 조회합니다.
     *
     * @param now 현재 시간
     * @return 발송 대기 중인 알림 목록
     */
    @Query("SELECT n FROM Notification n WHERE n.isSent = false AND n.scheduledAt <= :now")
    List<Notification> findPendingSend(@Param("now") LocalDateTime now);

    /**
     * 즉시 발송해야 할 알림들을 조회합니다.
     *
     * @return 즉시 발송 대기 중인 알림 목록
     */
    @Query("SELECT n FROM Notification n WHERE n.isSent = false AND n.scheduledAt IS NULL")
    List<Notification> findImmediateSend();

    /**
     * 푸시 알림 발송이 실패한 알림들을 조회합니다.
     *
     * @return 푸시 발송 실패 알림 목록
     */
    @Query("SELECT n FROM Notification n WHERE n.isSent = true AND n.isPushSent = false")
    List<Notification> findPendingPush();

    /**
     * 만료된 알림들을 조회합니다.
     *
     * @param now 현재 시간
     * @return 만료된 알림 목록
     */
    @Query("SELECT n FROM Notification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :now")
    List<Notification> findExpired(@Param("now") LocalDateTime now);

    /**
     * 특정 사용자의 특정 타입 알림을 조회합니다.
     *
     * @param user 사용자
     * @param type 알림 타입
     * @return 알림 목록
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.type = :type ORDER BY n.createdAt DESC")
    List<Notification> findByUserAndType(@Param("user") User user, @Param("type") String type);

    /**
     * 특정 기간 동안의 알림을 조회합니다.
     *
     * @param user      사용자
     * @param startDate 시작 날짜
     * @param endDate   종료 날짜
     * @return 알림 목록
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.createdAt BETWEEN :startDate AND :endDate ORDER BY n.createdAt DESC")
    List<Notification> findByUserAndDateRange(@Param("user") User user,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    /**
     * 특정 관련 데이터와 연결된 알림을 조회합니다.
     *
     * @param relatedDataId   관련 데이터 ID
     * @param relatedDataType 관련 데이터 타입
     * @return 관련 알림 목록
     */
    @Query("SELECT n FROM Notification n WHERE n.relatedDataId = :relatedDataId AND n.relatedDataType = :relatedDataType")
    List<Notification> findByRelatedData(@Param("relatedDataId") Long relatedDataId,
                                        @Param("relatedDataType") String relatedDataType);

    /**
     * 특정 우선순위의 알림을 조회합니다.
     *
     * @param priority 우선순위
     * @return 해당 우선순위의 알림 목록
     */
    @Query("SELECT n FROM Notification n WHERE n.priority = :priority ORDER BY n.createdAt DESC")
    List<Notification> findByPriority(@Param("priority") String priority);

    /**
     * 발송되지 않은 예약 알림 개수를 조회합니다.
     *
     * @return 발송 대기 중인 예약 알림 개수
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.isSent = false AND n.scheduledAt IS NOT NULL")
    long countPendingScheduled();

    /**
     * 특정 사용자의 오늘 알림 개수를 조회합니다.
     *
     * @param user      사용자
     * @param startDate 오늘 시작 시간
     * @param endDate   오늘 종료 시간
     * @return 오늘 알림 개수
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user = :user AND n.createdAt BETWEEN :startDate AND :endDate")
    long countTodayNotifications(@Param("user") User user,
                                 @Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    /**
     * 사용자의 모든 알림 조회 (최신순)
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user ORDER BY n.createdAt DESC")
    List<Notification> findByUser(@Param("user") User user);

    /**
     * 사용자의 알림 페이징 조회 (최신순)
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user ORDER BY n.createdAt DESC")
    Page<Notification> findByUser(@Param("user") User user, Pageable pageable);

    /**
     * 사용자의 읽지 않은 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByUser(@Param("user") User user);

    /**
     * 사용자의 읽지 않은 알림 개수 조회
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user = :user AND n.isRead = false")
    long countUnreadByUser(@Param("user") User user);

    /**
     * 사용자의 읽지 않은 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByUserId(@Param("userId") Long userId);

    /**
     * 사용자의 특정 타입 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.type = :type ORDER BY n.createdAt DESC")
    List<Notification> findByUserAndType(@Param("user") User user, @Param("type") NotificationType type);

    /**
     * 사용자의 특정 우선순위 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.priority = :priority ORDER BY n.createdAt DESC")
    List<Notification> findByUserAndPriority(@Param("user") User user, @Param("priority") Priority priority);

    /**
     * 발송 상태 업데이트
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isSent = true, n.sentAt = :sentAt WHERE n.id = :notificationId")
    int updateSentStatus(@Param("notificationId") Long notificationId,
                        @Param("sentAt") LocalDateTime sentAt);

    /**
     * 푸시 알림 발송 상태 업데이트
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isPushSent = true WHERE n.id = :notificationId")
    int updatePushSentStatus(@Param("notificationId") Long notificationId);

    /**
     * 알림 타입별 통계 조회
     */
    @Query("SELECT n.type, COUNT(n) FROM Notification n GROUP BY n.type ORDER BY COUNT(n) DESC")
    List<Object[]> getStatsByType();

    /**
     * 사용자별 알림 타입 통계 조회
     */
    @Query("SELECT n.type, COUNT(n) FROM Notification n WHERE n.user = :user GROUP BY n.type ORDER BY COUNT(n) DESC")
    List<Object[]> getUserStatsByType(@Param("user") User user);

    /**
     * 우선순위별 알림 통계 조회
     */
    @Query("SELECT n.priority, COUNT(n) FROM Notification n WHERE n.user = :user GROUP BY n.priority ORDER BY n.priority")
    List<Object[]> getUserStatsByPriority(@Param("user") User user);

    /**
     * 일별 알림 발송 통계
     */
    @Query("SELECT CAST(n.sentAt AS DATE) as date, COUNT(n) as count FROM Notification n WHERE n.isSent = true AND n.sentAt BETWEEN :startDate AND :endDate GROUP BY CAST(n.sentAt AS DATE) ORDER BY CAST(n.sentAt AS DATE)")
    List<Object[]> getDailySendStats(@Param("startDate") LocalDateTime startDate, 
                                    @Param("endDate") LocalDateTime endDate);

    /**
     * 사용자의 읽음률 계산
     */
    @Query("SELECT CAST(SUM(CASE WHEN n.isRead = true THEN 1 ELSE 0 END) AS DOUBLE) / COUNT(n) FROM Notification n WHERE n.user = :user")
    Double getReadRateByUser(@Param("user") User user);

    /**
     * 만료되지 않은 활성 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND (n.expiresAt IS NULL OR n.expiresAt > :now) ORDER BY n.createdAt DESC")
    List<Notification> findActive(@Param("user") User user, @Param("now") LocalDateTime now);

    /**
     * 높은 우선순위의 읽지 않은 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.isRead = false AND n.priority IN ('HIGH', 'URGENT') ORDER BY n.priority DESC, n.createdAt DESC")
    List<Notification> findHighPriorityUnread(@Param("user") User user);

    /**
     * 특정 타입의 발송되지 않은 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.type = :type AND n.isSent = false")
    List<Notification> findUnsentByType(@Param("type") NotificationType type);

    /**
     * 사용자의 총 알림 개수 조회
     */
    long countByUser(User user);

    /**
     * 여러 알림의 읽음 상태를 일괄 업데이트합니다.
     *
     * @param user 사용자
     * @param notificationIds 업데이트할 알림 ID 목록
     * @param isRead 읽음 상태
     * @param readAt 읽은 시간
     * @return 업데이트된 알림 수
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = :isRead, n.readAt = :readAt " +
           "WHERE n.user = :user AND n.id IN :notificationIds")
    int updateReadStatus(@Param("user") User user,
                        @Param("notificationIds") List<Long> notificationIds,
                        @Param("isRead") boolean isRead,
                        @Param("readAt") LocalDateTime readAt);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.user = :user")
    void deleteAllByUser(@Param("user") User user);
    
    /**
     * 발송 대기 중인 알림 개수 조회
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.isSent = false")
    int countPendingNotifications();
    
    /**
     * 특정 기간 동안 발송된 알림 개수 조회
     */
    int countByCreatedAtBetweenAndIsSentTrue(LocalDateTime start, LocalDateTime end);
    
    /**
     * 특정 기간 동안 발송 실패한 알림 개수 조회
     */
    int countByCreatedAtBetweenAndIsSentFalse(LocalDateTime start, LocalDateTime end);
    
    /**
     * 특정 타입의 알림이 특정 기간에 이미 발송되었는지 확인
     */
    boolean existsByUserAndTypeAndScheduledAtBetween(User user, NotificationType type, 
                                                     LocalDateTime startTime, LocalDateTime endTime);
    
    // Admin 기능을 위한 추가 메서드들
    
    /**
     * 사용자별 알림 수
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);
    
    /**
     * 고유 사용자 수
     */
    @Query("SELECT COUNT(DISTINCT n.user.id) FROM Notification n")
    Long countUniqueUsers();
} 