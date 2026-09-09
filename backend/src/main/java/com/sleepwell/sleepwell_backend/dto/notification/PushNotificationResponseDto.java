package com.sleepwell.sleepwell_backend.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 푸시 알림 발송 결과 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushNotificationResponseDto {
    
    /**
     * 발송 성공 여부
     */
    private boolean success;
    
    /**
     * Firebase 메시지 ID
     */
    private String messageId;
    
    /**
     * 오류 메시지 (실패 시)
     */
    private String errorMessage;
    
    /**
     * 오류 코드 (실패 시)
     */
    private String errorCode;
    
    /**
     * 대상 사용자 ID
     */
    private Long userId;
    
    /**
     * 대상 FCM 토큰
     */
    private String fcmToken;
    
    /**
     * 발송 시간
     */
    @Builder.Default
    private LocalDateTime sentAt = LocalDateTime.now();
    
    /**
     * 재시도 필요 여부
     */
    @Builder.Default
    private boolean retryable = false;
    
    /**
     * 토큰 무효화 여부 (토큰이 더 이상 유효하지 않은 경우)
     */
    @Builder.Default
    private boolean tokenInvalid = false;
    
    /**
     * 성공 응답 생성
     */
    public static PushNotificationResponseDto success(String messageId, Long userId, String fcmToken) {
        return PushNotificationResponseDto.builder()
                .success(true)
                .messageId(messageId)
                .userId(userId)
                .fcmToken(fcmToken)
                .build();
    }
    
    /**
     * 실패 응답 생성
     */
    public static PushNotificationResponseDto failure(String errorMessage, String errorCode, Long userId, String fcmToken) {
        return PushNotificationResponseDto.builder()
                .success(false)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .userId(userId)
                .fcmToken(fcmToken)
                .retryable(isRetryableError(errorCode))
                .tokenInvalid(isTokenInvalidError(errorCode))
                .build();
    }
    
    /**
     * 재시도 가능한 오류인지 판단
     */
    private static boolean isRetryableError(String errorCode) {
        return errorCode != null && (
                errorCode.equals("UNAVAILABLE") ||
                errorCode.equals("INTERNAL") ||
                errorCode.equals("DEADLINE_EXCEEDED")
        );
    }
    
    /**
     * 토큰 무효화 오류인지 판단
     */
    private static boolean isTokenInvalidError(String errorCode) {
        return errorCode != null && (
                errorCode.equals("UNREGISTERED") ||
                errorCode.equals("INVALID_ARGUMENT") ||
                errorCode.equals("NOT_FOUND")
        );
    }
}