package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.dto.notification.FcmTokenDto;
import com.sleepwell.sleepwell_backend.dto.notification.PushNotificationRequestDto;
import com.sleepwell.sleepwell_backend.dto.notification.PushNotificationResponseDto;

import java.util.List;
import java.util.Map;

/**
 * FCM(Firebase Cloud Messaging) 푸시 알림 서비스 인터페이스
 * 푸시 알림 발송, 토큰 관리, 토픽 구독 등의 기능을 제공합니다.
 */
public interface FcmPushService {
    
    /**
     * 단일 사용자에게 푸시 알림을 발송합니다.
     *
     * @param userId 사용자 ID
     * @param title 알림 제목
     * @param body 알림 내용
     * @param data 추가 데이터 (선택사항)
     * @return 발송 결과
     */
    PushNotificationResponseDto sendToUser(Long userId, String title, String body, Map<String, String> data);
    
    /**
     * FCM 토큰을 사용하여 직접 푸시 알림을 발송합니다.
     *
     * @param fcmToken FCM 토큰
     * @param title 알림 제목
     * @param body 알림 내용
     * @param data 추가 데이터 (선택사항)
     * @return 발송 결과
     */
    PushNotificationResponseDto sendToToken(String fcmToken, String title, String body, Map<String, String> data);
    
    /**
     * 여러 사용자에게 동시에 푸시 알림을 발송합니다.
     *
     * @param userIds 사용자 ID 목록
     * @param title 알림 제목
     * @param body 알림 내용
     * @param data 추가 데이터 (선택사항)
     * @return 각 사용자별 발송 결과
     */
    List<PushNotificationResponseDto> sendToMultipleUsers(List<Long> userIds, String title, String body, Map<String, String> data);
    
    /**
     * 특정 토픽을 구독하는 모든 사용자에게 푸시 알림을 발송합니다.
     *
     * @param topic 토픽 이름
     * @param title 알림 제목
     * @param body 알림 내용
     * @param data 추가 데이터 (선택사항)
     * @return 발송 결과
     */
    PushNotificationResponseDto sendToTopic(String topic, String title, String body, Map<String, String> data);
    
    /**
     * 조건에 맞는 사용자들에게 푸시 알림을 발송합니다.
     *
     * @param condition FCM 조건식 (예: "'stock-GOOG' in topics || 'industry-tech' in topics")
     * @param title 알림 제목
     * @param body 알림 내용
     * @param data 추가 데이터 (선택사항)
     * @return 발송 결과
     */
    PushNotificationResponseDto sendToCondition(String condition, String title, String body, Map<String, String> data);
    
    /**
     * 푸시 알림 요청 DTO를 사용하여 알림을 발송합니다.
     *
     * @param request 푸시 알림 요청 정보
     * @return 발송 결과
     */
    PushNotificationResponseDto send(PushNotificationRequestDto request);
    
    /**
     * 사용자의 FCM 토큰을 업데이트합니다.
     *
     * @param userId 사용자 ID
     * @param fcmToken 새로운 FCM 토큰
     * @return 업데이트된 토큰 정보
     */
    FcmTokenDto updateUserFcmToken(Long userId, String fcmToken);
    
    /**
     * 사용자의 FCM 토큰을 삭제합니다.
     *
     * @param userId 사용자 ID
     */
    void deleteUserFcmToken(Long userId);
    
    /**
     * 사용자를 특정 토픽에 구독시킵니다.
     *
     * @param userId 사용자 ID
     * @param topic 토픽 이름
     */
    void subscribeToTopic(Long userId, String topic);
    
    /**
     * 여러 사용자를 특정 토픽에 구독시킵니다.
     *
     * @param userIds 사용자 ID 목록
     * @param topic 토픽 이름
     */
    void subscribeMultipleToTopic(List<Long> userIds, String topic);
    
    /**
     * 사용자를 특정 토픽에서 구독 해제합니다.
     *
     * @param userId 사용자 ID
     * @param topic 토픽 이름
     */
    void unsubscribeFromTopic(Long userId, String topic);
    
    /**
     * 여러 사용자를 특정 토픽에서 구독 해제합니다.
     *
     * @param userIds 사용자 ID 목록
     * @param topic 토픽 이름
     */
    void unsubscribeMultipleFromTopic(List<Long> userIds, String topic);
    
    /**
     * FCM 토큰의 유효성을 검증합니다.
     *
     * @param fcmToken 검증할 FCM 토큰
     * @return 토큰 유효 여부
     */
    boolean validateToken(String fcmToken);
    
    /**
     * 사용자가 특정 토픽을 구독 중인지 확인합니다.
     *
     * @param userId 사용자 ID
     * @param topic 토픽 이름
     * @return 구독 여부
     */
    boolean isSubscribedToTopic(Long userId, String topic);
    
    /**
     * 사용자가 구독 중인 모든 토픽 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 구독 중인 토픽 목록
     */
    List<String> getUserTopics(Long userId);
}