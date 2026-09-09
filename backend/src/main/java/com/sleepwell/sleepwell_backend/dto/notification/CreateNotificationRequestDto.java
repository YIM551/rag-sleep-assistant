package com.sleepwell.sleepwell_backend.dto.notification;

import com.sleepwell.sleepwell_backend.enums.NotificationType;
import com.sleepwell.sleepwell_backend.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 생성 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationRequestDto {
    
    @NotNull(message = "알림 타입은 필수입니다")
    private NotificationType type;
    
    @NotBlank(message = "알림 제목은 필수입니다")
    private String title;
    
    @NotBlank(message = "알림 내용은 필수입니다")
    private String content;
    
    private Priority priority;
    
    // 예약 알림용
    private LocalDateTime scheduledAt;
    
    private LocalDateTime expiresAt;
    
    // 연관 데이터용
    private Long relatedId;
    
    private String relatedType;
}