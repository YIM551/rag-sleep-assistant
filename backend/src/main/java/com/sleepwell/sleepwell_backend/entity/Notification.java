package com.sleepwell.sleepwell_backend.entity;

import com.sleepwell.sleepwell_backend.enums.NotificationType;
import com.sleepwell.sleepwell_backend.enums.Priority;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "NOTIFICATIONS", indexes = {
        @Index(name = "idx_notification_user_created_at", columnList = "user_id, created_at DESC"),
        @Index(name = "idx_notification_scheduled_at", columnList = "scheduledAt, isSent"),
        @Index(name = "idx_notification_expires_at", columnList = "expiresAt"),
        @Index(name = "idx_notification_push_pending", columnList = "isPushSent, isSent")
})
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "type", length = 50)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    private LocalDateTime readAt;

    // === 스케줄링 관련 필드들 ===
    
    /**
     * 알림 발송 예약 시간
     * null인 경우 즉시 발송
     */
    private LocalDateTime scheduledAt;

    /**
     * 알림 발송 완료 여부
     */
    @Builder.Default
    @Column(name = "is_sent", nullable = false)
    private boolean isSent = false;

    /**
     * 알림 발송 시간
     */
    private LocalDateTime sentAt;

    /**
     * 푸시 알림 발송 완료 여부
     */
    @Builder.Default
    @Column(name = "is_push_sent", nullable = false)
    private boolean isPushSent = false;

    /**
     * 알림 만료 시간
     */
    private LocalDateTime expiresAt;

    /**
     * 알림 우선순위
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.NORMAL;

    /**
     * 관련 데이터 ID (수면 기록, 분석 결과 등)
     */
    private Long relatedDataId;

    /**
     * 관련 데이터 타입
     */
    private String relatedDataType;

    /**
     * 알림 클릭 시 앱 내 이동할 화면의 URL
     */
    private String deepLinkUrl;

    /**
     * 알림에 표시될 액션 버튼들의 JSON 배열
     * 예: [{"text": "상담 예약", "action": "BOOK_CONSULTATION"}]
     */
    @Column(columnDefinition = "TEXT")
    private String actionButtons;

    public void markAsRead() {
        if (!this.isRead) {
            this.isRead = true;
            this.readAt = LocalDateTime.now();
        }
    }

    public void markAsSent() {
        if (!this.isSent) {
            this.isSent = true;
            this.sentAt = LocalDateTime.now();
        }
    }

    public void markAsPushSent() {
        this.isPushSent = true;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isScheduledForNow() {
        return scheduledAt == null || scheduledAt.isBefore(LocalDateTime.now()) || scheduledAt.isEqual(LocalDateTime.now());
    }
} 