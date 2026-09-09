package com.sleepwell.sleepwell_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * FCM 토픽 구독 정보 엔티티
 * 
 * 사용자의 FCM 토픽 구독 상태를 추적하고 관리합니다.
 * Firebase Admin SDK는 토픽 구독 상태를 직접 조회하는 API를 제공하지 않으므로,
 * 애플리케이션 레벨에서 구독 정보를 관리해야 합니다.
 */
@Entity
@Table(name = "topic_subscriptions", 
    indexes = {
        @Index(name = "idx_topic_subscription_user_topic", columnList = "user_id, topic", unique = true),
        @Index(name = "idx_topic_subscription_user", columnList = "user_id"),
        @Index(name = "idx_topic_subscription_topic", columnList = "topic")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@ToString(exclude = {"user"})
public class TopicSubscription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 구독한 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * FCM 토픽 이름
     */
    @Column(nullable = false, length = 100)
    private String topic;
    
    /**
     * 구독 상태 (true: 구독 중, false: 구독 취소)
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    /**
     * 구독 시작 시간
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime subscribedAt;
    
    /**
     * 구독 취소 시간 (구독 취소 시에만 설정)
     */
    private LocalDateTime unsubscribedAt;
    
    /**
     * 마지막 알림 발송 시간
     */
    private LocalDateTime lastNotificationAt;
    
    /**
     * 알림 발송 횟수
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer notificationCount = 0;
    
    /**
     * 구독 취소 처리
     */
    public void unsubscribe() {
        this.isActive = false;
        this.unsubscribedAt = LocalDateTime.now();
    }
    
    /**
     * 재구독 처리
     */
    public void resubscribe() {
        this.isActive = true;
        this.unsubscribedAt = null;
    }
    
    /**
     * 알림 발송 기록 업데이트
     */
    public void recordNotification() {
        this.lastNotificationAt = LocalDateTime.now();
        this.notificationCount++;
    }
}