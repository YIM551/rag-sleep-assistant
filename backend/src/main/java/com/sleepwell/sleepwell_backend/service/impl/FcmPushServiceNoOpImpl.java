package com.sleepwell.sleepwell_backend.service.impl;

import com.sleepwell.sleepwell_backend.dto.notification.FcmTokenDto;
import com.sleepwell.sleepwell_backend.dto.notification.PushNotificationRequestDto;
import com.sleepwell.sleepwell_backend.dto.notification.PushNotificationResponseDto;
import com.sleepwell.sleepwell_backend.service.FcmPushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * FCM 푸시 알림 서비스 No-Op 구현체
 * Firebase가 비활성화되어 있을 때 사용되는 구현체로, 실제 푸시 알림을 발송하지 않고 로그만 출력합니다.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "false", matchIfMissing = true)
public class FcmPushServiceNoOpImpl implements FcmPushService {
    
    @Override
    public PushNotificationResponseDto sendToUser(Long userId, String title, String body, Map<String, String> data) {
        log.debug("FCM 미설정 - 푸시 알림 발송 시뮬레이션: userId={}, title={}", userId, title);
        return PushNotificationResponseDto.success("SIMULATED-" + System.currentTimeMillis(), userId, null);
    }
    
    @Override
    public PushNotificationResponseDto sendToToken(String fcmToken, String title, String body, Map<String, String> data) {
        log.debug("FCM 미설정 - 푸시 알림 발송 시뮬레이션: token={}, title={}", fcmToken, title);
        return PushNotificationResponseDto.success("SIMULATED-" + System.currentTimeMillis(), null, fcmToken);
    }
    
    @Override
    public List<PushNotificationResponseDto> sendToMultipleUsers(List<Long> userIds, String title, String body, Map<String, String> data) {
        log.debug("FCM 미설정 - 다중 푸시 알림 발송 시뮬레이션: userCount={}, title={}", userIds.size(), title);
        return userIds.stream()
                .map(userId -> PushNotificationResponseDto.success("SIMULATED-" + System.currentTimeMillis(), userId, null))
                .collect(Collectors.toList());
    }
    
    @Override
    public PushNotificationResponseDto sendToTopic(String topic, String title, String body, Map<String, String> data) {
        log.debug("FCM 미설정 - 토픽 푸시 알림 발송 시뮬레이션: topic={}, title={}", topic, title);
        return PushNotificationResponseDto.success("SIMULATED-" + System.currentTimeMillis(), null, null);
    }
    
    @Override
    public PushNotificationResponseDto sendToCondition(String condition, String title, String body, Map<String, String> data) {
        log.debug("FCM 미설정 - 조건부 푸시 알림 발송 시뮬레이션: condition={}, title={}", condition, title);
        return PushNotificationResponseDto.success("SIMULATED-" + System.currentTimeMillis(), null, null);
    }
    
    @Override
    public PushNotificationResponseDto send(PushNotificationRequestDto request) {
        log.debug("FCM 미설정 - 푸시 알림 발송 시뮬레이션: title={}", request.getTitle());
        
        if (request.getUserId() != null) {
            return sendToUser(request.getUserId(), request.getTitle(), request.getBody(), request.getData());
        } else if (request.getFcmToken() != null) {
            return sendToToken(request.getFcmToken(), request.getTitle(), request.getBody(), request.getData());
        } else if (request.getTopic() != null) {
            return sendToTopic(request.getTopic(), request.getTitle(), request.getBody(), request.getData());
        } else if (request.getCondition() != null) {
            return sendToCondition(request.getCondition(), request.getTitle(), request.getBody(), request.getData());
        }
        
        return PushNotificationResponseDto.failure("발송 대상을 지정해주세요.", "NO_TARGET", null, null);
    }
    
    @Override
    public FcmTokenDto updateUserFcmToken(Long userId, String fcmToken) {
        log.debug("FCM 미설정 - FCM 토큰 업데이트 시뮬레이션: userId={}, token={}", userId, fcmToken);
        return FcmTokenDto.builder()
                .userId(userId)
                .fcmToken(fcmToken)
                .updatedAt(LocalDateTime.now())
                .isActive(true)
                .build();
    }
    
    @Override
    public void deleteUserFcmToken(Long userId) {
        log.debug("FCM 미설정 - FCM 토큰 삭제 시뮬레이션: userId={}", userId);
    }
    
    @Override
    public void subscribeToTopic(Long userId, String topic) {
        log.debug("FCM 미설정 - 토픽 구독 시뮬레이션: userId={}, topic={}", userId, topic);
    }
    
    @Override
    public void subscribeMultipleToTopic(List<Long> userIds, String topic) {
        log.debug("FCM 미설정 - 다중 토픽 구독 시뮬레이션: userCount={}, topic={}", userIds.size(), topic);
    }
    
    @Override
    public void unsubscribeFromTopic(Long userId, String topic) {
        log.debug("FCM 미설정 - 토픽 구독 해제 시뮬레이션: userId={}, topic={}", userId, topic);
    }
    
    @Override
    public void unsubscribeMultipleFromTopic(List<Long> userIds, String topic) {
        log.debug("FCM 미설정 - 다중 토픽 구독 해제 시뮬레이션: userCount={}, topic={}", userIds.size(), topic);
    }
    
    @Override
    public boolean validateToken(String fcmToken) {
        log.debug("FCM 미설정 - 토큰 검증 시뮬레이션: token={}", fcmToken);
        return fcmToken != null && fcmToken.length() > 10;
    }
    
    @Override
    public boolean isSubscribedToTopic(Long userId, String topic) {
        log.debug("FCM 미설정 - 토픽 구독 상태 확인 시뮬레이션: userId={}, topic={}", userId, topic);
        return false;
    }
    
    @Override
    public List<String> getUserTopics(Long userId) {
        log.debug("FCM 미설정 - 사용자 토픽 목록 조회 시뮬레이션: userId={}", userId);
        return Collections.emptyList();
    }
}