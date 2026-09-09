package com.sleepwell.sleepwell_backend.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * FCM 토큰 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FcmTokenDto {
    
    /**
     * 사용자 ID
     */
    private Long userId;
    
    /**
     * FCM 토큰
     */
    private String fcmToken;
    
    /**
     * 토큰 생성/업데이트 시간
     */
    private LocalDateTime updatedAt;
    
    /**
     * 토큰 활성화 여부
     */
    private boolean isActive;
    
    /**
     * 디바이스 정보 (선택사항)
     */
    private String deviceInfo;
    
    /**
     * 플랫폼 (iOS, Android, Web)
     */
    private String platform;
}