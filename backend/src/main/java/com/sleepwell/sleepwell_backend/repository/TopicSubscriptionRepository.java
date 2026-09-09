package com.sleepwell.sleepwell_backend.repository;

import com.sleepwell.sleepwell_backend.entity.TopicSubscription;
import com.sleepwell.sleepwell_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * FCM 토픽 구독 정보 레포지토리
 */
@Repository
public interface TopicSubscriptionRepository extends JpaRepository<TopicSubscription, Long> {
    
    /**
     * 사용자와 토픽으로 구독 정보 조회
     */
    Optional<TopicSubscription> findByUserAndTopic(User user, String topic);
    
    /**
     * 사용자 ID와 토픽으로 구독 정보 조회
     */
    @Query("SELECT ts FROM TopicSubscription ts WHERE ts.user.id = :userId AND ts.topic = :topic")
    Optional<TopicSubscription> findByUserIdAndTopic(@Param("userId") Long userId, @Param("topic") String topic);
    
    /**
     * 사용자의 활성 구독 목록 조회
     */
    List<TopicSubscription> findByUserAndIsActiveTrue(User user);
    
    /**
     * 사용자 ID로 활성 구독 목록 조회
     */
    @Query("SELECT ts FROM TopicSubscription ts WHERE ts.user.id = :userId AND ts.isActive = true")
    List<TopicSubscription> findActiveByUserId(@Param("userId") Long userId);
    
    /**
     * 사용자의 모든 구독 목록 조회 (활성/비활성 포함)
     */
    List<TopicSubscription> findByUser(User user);
    
    /**
     * 특정 토픽의 활성 구독자 수 조회
     */
    @Query("SELECT COUNT(ts) FROM TopicSubscription ts WHERE ts.topic = :topic AND ts.isActive = true")
    Long countActiveSubscribers(@Param("topic") String topic);
    
    /**
     * 특정 토픽의 활성 구독 목록 조회
     */
    List<TopicSubscription> findByTopicAndIsActiveTrue(String topic);
    
    /**
     * 사용자가 특정 토픽을 구독 중인지 확인
     */
    @Query("SELECT CASE WHEN COUNT(ts) > 0 THEN true ELSE false END FROM TopicSubscription ts " +
           "WHERE ts.user.id = :userId AND ts.topic = :topic AND ts.isActive = true")
    boolean isUserSubscribedToTopic(@Param("userId") Long userId, @Param("topic") String topic);
    
    /**
     * 사용자의 활성 토픽 이름 목록 조회
     */
    @Query("SELECT ts.topic FROM TopicSubscription ts WHERE ts.user.id = :userId AND ts.isActive = true")
    List<String> findActiveTopicsByUserId(@Param("userId") Long userId);
    
    /**
     * 알림 발송 기록 업데이트
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE TopicSubscription ts SET ts.lastNotificationAt = :timestamp, " +
           "ts.notificationCount = ts.notificationCount + 1 " +
           "WHERE ts.user.id = :userId AND ts.topic = :topic")
    void updateNotificationRecord(@Param("userId") Long userId,
                                  @Param("topic") String topic,
                                  @Param("timestamp") LocalDateTime timestamp);
    
    /**
     * 토픽의 모든 구독자에 대한 알림 발송 기록 업데이트
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE TopicSubscription ts SET ts.lastNotificationAt = :timestamp, " +
           "ts.notificationCount = ts.notificationCount + 1 " +
           "WHERE ts.topic = :topic AND ts.isActive = true")
    void updateTopicNotificationRecord(@Param("topic") String topic,
                                      @Param("timestamp") LocalDateTime timestamp);
    
    /**
     * 특정 기간 동안 활동이 없는 구독 조회
     */
    @Query("SELECT ts FROM TopicSubscription ts WHERE ts.isActive = true " +
           "AND (ts.lastNotificationAt IS NULL OR ts.lastNotificationAt < :cutoffDate)")
    List<TopicSubscription> findInactiveSubscriptions(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * 사용자의 모든 구독 비활성화
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE TopicSubscription ts SET ts.isActive = false, ts.unsubscribedAt = :timestamp " +
           "WHERE ts.user.id = :userId AND ts.isActive = true")
    void deactivateAllUserSubscriptions(@Param("userId") Long userId, @Param("timestamp") LocalDateTime timestamp);
}