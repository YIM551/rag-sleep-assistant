package com.sleepwell.sleepwell_backend.dto.notification;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 푸시 알림 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushNotificationRequestDto {
    
    /**
     * 알림 제목
     */
    @NotBlank(message = "알림 제목은 필수입니다")
    private String title;
    
    /**
     * 알림 내용
     */
    @NotBlank(message = "알림 내용은 필수입니다")
    private String body;
    
    /**
     * 대상 사용자 ID (단일 사용자)
     */
    private Long userId;
    
    /**
     * 대상 사용자 ID 목록 (다중 사용자)
     */
    private List<Long> userIds;
    
    /**
     * FCM 토큰 (직접 발송 시)
     */
    private String fcmToken;
    
    /**
     * FCM 토큰 목록 (다중 직접 발송 시)
     */
    private List<String> fcmTokens;
    
    /**
     * 토픽 이름 (토픽 발송 시)
     */
    private String topic;
    
    /**
     * 조건식 (조건부 발송 시)
     */
    private String condition;
    
    /**
     * 추가 데이터 페이로드
     */
    private Map<String, String> data;
    
    /**
     * 알림 이미지 URL (선택사항)
     */
    private String imageUrl;
    
    /**
     * 알림 클릭 시 이동할 URL/딥링크 (선택사항)
     */
    private String clickAction;
    
    /**
     * 알림 아이콘 (Android)
     */
    private String icon;
    
    /**
     * 알림 색상 (Android)
     */
    private String color;
    
    /**
     * 알림 사운드
     */
    private String sound;
    
    /**
     * 알림 배지 수 (iOS)
     */
    private Integer badge;
    
    /**
     * 우선순위 (high, normal)
     */
    @Builder.Default
    private String priority = "high";
    
    /**
     * TTL (Time To Live) - 초 단위
     */
    @Builder.Default
    private Integer ttl = 86400; // 24시간
    
    /**
     * 무음 알림 여부
     */
    @Builder.Default
    private boolean silent = false;
    
    /**
     * 분석 라벨 (Firebase Analytics)
     */
    private String analyticsLabel;
}