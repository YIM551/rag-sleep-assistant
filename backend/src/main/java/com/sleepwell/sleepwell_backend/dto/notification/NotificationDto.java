package com.sleepwell.sleepwell_backend.dto.notification;

import com.sleepwell.sleepwell_backend.entity.Notification;
import com.sleepwell.sleepwell_backend.enums.NotificationType;
import com.sleepwell.sleepwell_backend.enums.Priority;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NotificationDto {

    private final Long id;
    private final NotificationType type;
    private final String title;
    private final String message;
    private final boolean isRead;
    private final LocalDateTime createdAt;
    private final LocalDateTime readAt;
    
    // 스케줄링 관련 필드들
    private final LocalDateTime scheduledAt;
    private final boolean isSent;
    private final LocalDateTime sentAt;
    private final boolean isPushSent;
    private final LocalDateTime expiresAt;
    private final Priority priority;
    private final Long relatedDataId;
    private final String relatedDataType;
    private final String deepLinkUrl;
    private final String actionButtons;

    public NotificationDto(Notification notification) {
        this.id = notification.getId();
        this.type = notification.getType();
        this.title = notification.getTitle();
        this.message = notification.getMessage();
        this.isRead = notification.isRead();
        this.createdAt = notification.getCreatedAt();
        this.readAt = notification.getReadAt();
        this.scheduledAt = notification.getScheduledAt();
        this.isSent = notification.isSent();
        this.sentAt = notification.getSentAt();
        this.isPushSent = notification.isPushSent();
        this.expiresAt = notification.getExpiresAt();
        this.priority = notification.getPriority();
        this.relatedDataId = notification.getRelatedDataId();
        this.relatedDataType = notification.getRelatedDataType();
        this.deepLinkUrl = notification.getDeepLinkUrl();
        this.actionButtons = notification.getActionButtons();
    }
} 