package com.sleepwell.sleepwell_backend.service.impl;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import com.sleepwell.sleepwell_backend.dto.notification.FcmTokenDto;
import com.sleepwell.sleepwell_backend.dto.notification.PushNotificationRequestDto;
import com.sleepwell.sleepwell_backend.dto.notification.PushNotificationResponseDto;
import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.entity.TopicSubscription;
import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.exception.FcmException;
import com.sleepwell.sleepwell_backend.repository.UserRepository;
import com.sleepwell.sleepwell_backend.repository.TopicSubscriptionRepository;
import com.sleepwell.sleepwell_backend.service.FcmPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * FCM 푸시 알림 서비스 구현체
 * Firebase Cloud Messaging을 사용하여 푸시 알림을 발송합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
public class FcmPushServiceImpl implements FcmPushService {

    private final UserRepository userRepository;
    private final TopicSubscriptionRepository topicSubscriptionRepository;
    
    /**
     * 단일 사용자에게 푸시 알림을 발송합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public PushNotificationResponseDto sendToUser(Long userId, String title, String body, Map<String, String> data) {
        User user = findUserById(userId);
        
        if (!StringUtils.hasText(user.getFcmToken())) {
            log.warn("사용자 {}의 FCM 토큰이 없습니다.", userId);
            return PushNotificationResponseDto.failure("FCM 토큰이 없습니다", "NO_TOKEN", userId, null);
        }
        
        return sendToToken(user.getFcmToken(), title, body, data);
    }
    
    /**
     * FCM 토큰을 사용하여 직접 푸시 알림을 발송합니다.
     */
    @Override
    public PushNotificationResponseDto sendToToken(String fcmToken, String title, String body, Map<String, String> data) {
        try {
            // Firebase 앱 초기화 확인
            if (FirebaseApp.getApps().isEmpty()) {
                throw new FcmException.NotInitializedException("Firebase가 초기화되지 않았습니다. Firebase 설정을 확인해주세요.");
            }
            
            // 메시지 빌더 생성
            Message.Builder messageBuilder = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());
            
            // 추가 데이터가 있으면 포함
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }
            
            // Android 특화 설정
            messageBuilder.setAndroidConfig(AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification.builder()
                            .setClickAction("FLUTTER_NOTIFICATION_CLICK")
                            .build())
                    .build());
            
            // iOS 특화 설정
            messageBuilder.setApnsConfig(ApnsConfig.builder()
                    .setAps(Aps.builder()
                            .setAlert(ApsAlert.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .build())
                            .setSound("default")
                            .build())
                    .build());
            
            // 메시지 발송
            String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
            
            log.info("FCM 푸시 알림 발송 성공: messageId={}", response);
            return PushNotificationResponseDto.success(response, null, fcmToken);
            
        } catch (FirebaseMessagingException e) {
            log.error("FCM 푸시 알림 발송 실패: token={}, error={}", fcmToken, e.getMessage(), e);
            return PushNotificationResponseDto.failure(
                    e.getMessage(), 
                    e.getMessagingErrorCode() != null ? e.getMessagingErrorCode().name() : "UNKNOWN",
                    null,
                    fcmToken
            );
        } catch (Exception e) {
            log.error("FCM 푸시 알림 발송 중 예상치 못한 오류: token={}, error={}", fcmToken, e.getMessage(), e);
            return PushNotificationResponseDto.failure(e.getMessage(), "INTERNAL_ERROR", null, fcmToken);
        }
    }
    
    /**
     * 여러 사용자에게 동시에 푸시 알림을 발송합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public List<PushNotificationResponseDto> sendToMultipleUsers(List<Long> userIds, String title, String body, Map<String, String> data) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 사용자들의 FCM 토큰 조회
        List<User> users = userRepository.findAllById(userIds);
        List<String> tokens = users.stream()
                .filter(user -> StringUtils.hasText(user.getFcmToken()))
                .map(User::getFcmToken)
                .collect(Collectors.toList());
        
        if (tokens.isEmpty()) {
            log.warn("발송 대상 사용자들의 FCM 토큰이 없습니다.");
            return userIds.stream()
                    .map(userId -> PushNotificationResponseDto.failure("FCM 토큰이 없습니다", "NO_TOKEN", userId, null))
                    .collect(Collectors.toList());
        }
        
        try {
            // 멀티캐스트 메시지 생성
            MulticastMessage multicastMessage = MulticastMessage.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data != null ? data : Collections.emptyMap())
                    .addAllTokens(tokens)
                    .build();
            
            // 배치 발송
            BatchResponse batchResponse = FirebaseMessaging.getInstance().sendMulticast(multicastMessage);
            
            // 발송 결과 처리
            List<PushNotificationResponseDto> results = new ArrayList<>();
            List<SendResponse> responses = batchResponse.getResponses();
            
            for (int i = 0; i < responses.size(); i++) {
                SendResponse response = responses.get(i);
                String token = tokens.get(i);
                Long userId = users.get(i).getId();
                
                if (response.isSuccessful()) {
                    results.add(PushNotificationResponseDto.success(response.getMessageId(), userId, token));
                } else {
                    FirebaseMessagingException exception = response.getException();
                    results.add(PushNotificationResponseDto.failure(
                            exception.getMessage(),
                            exception.getMessagingErrorCode() != null ? exception.getMessagingErrorCode().name() : "UNKNOWN",
                            userId,
                            token
                    ));
                }
            }
            
            log.info("멀티캐스트 발송 완료: 성공={}, 실패={}", batchResponse.getSuccessCount(), batchResponse.getFailureCount());
            return results;
            
        } catch (FirebaseMessagingException e) {
            log.error("멀티캐스트 발송 실패: error={}", e.getMessage(), e);
            return tokens.stream()
                    .map(token -> PushNotificationResponseDto.failure(e.getMessage(), "MULTICAST_ERROR", null, token))
                    .collect(Collectors.toList());
        }
    }
    
    /**
     * 특정 토픽을 구독하는 모든 사용자에게 푸시 알림을 발송합니다.
     */
    @Override
    @Transactional
    public PushNotificationResponseDto sendToTopic(String topic, String title, String body, Map<String, String> data) {
        try {
            Message.Builder messageBuilder = Message.builder()
                    .setTopic(topic)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());
            
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }
            
            String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
            
            log.info("토픽 푸시 알림 발송 성공: topic={}, messageId={}", topic, response);
            
            // 토픽 구독자들의 알림 발송 기록 업데이트
            topicSubscriptionRepository.updateTopicNotificationRecord(topic, LocalDateTime.now());
            
            return PushNotificationResponseDto.success(response, null, null);
            
        } catch (FirebaseMessagingException e) {
            log.error("토픽 푸시 알림 발송 실패: topic={}, error={}", topic, e.getMessage(), e);
            return PushNotificationResponseDto.failure(e.getMessage(), "TOPIC_ERROR", null, null);
        }
    }
    
    /**
     * 조건에 맞는 사용자들에게 푸시 알림을 발송합니다.
     */
    @Override
    public PushNotificationResponseDto sendToCondition(String condition, String title, String body, Map<String, String> data) {
        try {
            Message.Builder messageBuilder = Message.builder()
                    .setCondition(condition)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());
            
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }
            
            String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
            
            log.info("조건부 푸시 알림 발송 성공: condition={}, messageId={}", condition, response);
            return PushNotificationResponseDto.success(response, null, null);
            
        } catch (FirebaseMessagingException e) {
            log.error("조건부 푸시 알림 발송 실패: condition={}, error={}", condition, e.getMessage(), e);
            return PushNotificationResponseDto.failure(e.getMessage(), "CONDITION_ERROR", null, null);
        }
    }
    
    /**
     * 푸시 알림 요청 DTO를 사용하여 알림을 발송합니다.
     */
    @Override
    public PushNotificationResponseDto send(PushNotificationRequestDto request) {
        // 검증
        if (!StringUtils.hasText(request.getTitle()) || !StringUtils.hasText(request.getBody())) {
            throw new BusinessException("제목과 내용은 필수입니다.", HttpStatus.BAD_REQUEST);
        }
        
        // 발송 타입에 따라 처리
        if (request.getUserId() != null) {
            return sendToUser(request.getUserId(), request.getTitle(), request.getBody(), request.getData());
        } else if (StringUtils.hasText(request.getFcmToken())) {
            return sendToToken(request.getFcmToken(), request.getTitle(), request.getBody(), request.getData());
        } else if (StringUtils.hasText(request.getTopic())) {
            return sendToTopic(request.getTopic(), request.getTitle(), request.getBody(), request.getData());
        } else if (StringUtils.hasText(request.getCondition())) {
            return sendToCondition(request.getCondition(), request.getTitle(), request.getBody(), request.getData());
        } else {
            throw new BusinessException("발송 대상을 지정해주세요.", HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * 사용자의 FCM 토큰을 업데이트합니다.
     */
    @Override
    @Transactional
    public FcmTokenDto updateUserFcmToken(Long userId, String fcmToken) {
        User user = findUserById(userId);
        
        // 토큰 유효성 검증
        if (!validateToken(fcmToken)) {
            throw new FcmException.InvalidTokenException("유효하지 않은 FCM 토큰입니다.");
        }
        
        user.updateFcmToken(fcmToken);
        userRepository.save(user);
        
        log.info("사용자 {}의 FCM 토큰을 업데이트했습니다.", userId);
        
        return FcmTokenDto.builder()
                .userId(userId)
                .fcmToken(fcmToken)
                .updatedAt(LocalDateTime.now())
                .isActive(true)
                .build();
    }
    
    /**
     * 사용자의 FCM 토큰을 삭제합니다.
     */
    @Override
    @Transactional
    public void deleteUserFcmToken(Long userId) {
        User user = findUserById(userId);
        user.updateFcmToken(null);
        userRepository.save(user);
        
        log.info("사용자 {}의 FCM 토큰을 삭제했습니다.", userId);
    }
    
    /**
     * 사용자를 특정 토픽에 구독시킵니다.
     */
    @Override
    @Transactional
    public void subscribeToTopic(Long userId, String topic) {
        User user = findUserById(userId);
        
        if (!StringUtils.hasText(user.getFcmToken())) {
            throw new BusinessException("FCM 토큰이 없습니다.", HttpStatus.BAD_REQUEST);
        }
        
        try {
            TopicManagementResponse response = FirebaseMessaging.getInstance()
                    .subscribeToTopic(Collections.singletonList(user.getFcmToken()), topic);
            
            log.info("토픽 구독 성공: userId={}, topic={}, successCount={}", 
                    userId, topic, response.getSuccessCount());
            
            // 구독 정보를 데이터베이스에 저장
            Optional<TopicSubscription> existingSubscription = topicSubscriptionRepository.findByUserAndTopic(user, topic);
            if (existingSubscription.isPresent()) {
                // 기존 구독이 있으면 재활성화
                TopicSubscription subscription = existingSubscription.get();
                subscription.resubscribe();
                topicSubscriptionRepository.save(subscription);
            } else {
                // 신규 구독 생성
                TopicSubscription subscription = TopicSubscription.builder()
                        .user(user)
                        .topic(topic)
                        .isActive(true)
                        .build();
                topicSubscriptionRepository.save(subscription);
            }
            
        } catch (FirebaseMessagingException e) {
            log.error("토픽 구독 실패: userId={}, topic={}, error={}", userId, topic, e.getMessage(), e);
            throw new FcmException.TopicOperationException("토픽 구독에 실패했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 여러 사용자를 특정 토픽에 구독시킵니다.
     */
    @Override
    @Transactional(readOnly = true)
    public void subscribeMultipleToTopic(List<Long> userIds, String topic) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        
        List<User> users = userRepository.findAllById(userIds);
        List<String> tokens = users.stream()
                .filter(user -> StringUtils.hasText(user.getFcmToken()))
                .map(User::getFcmToken)
                .collect(Collectors.toList());
        
        if (tokens.isEmpty()) {
            log.warn("구독 대상 사용자들의 FCM 토큰이 없습니다.");
            return;
        }
        
        try {
            TopicManagementResponse response = FirebaseMessaging.getInstance()
                    .subscribeToTopic(tokens, topic);
            
            log.info("다중 토픽 구독 성공: topic={}, successCount={}, failureCount={}", 
                    topic, response.getSuccessCount(), response.getFailureCount());
            
        } catch (FirebaseMessagingException e) {
            log.error("다중 토픽 구독 실패: topic={}, error={}", topic, e.getMessage(), e);
            throw new FcmException.TopicOperationException("토픽 구독에 실패했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 사용자를 특정 토픽에서 구독 해제합니다.
     */
    @Override
    @Transactional
    public void unsubscribeFromTopic(Long userId, String topic) {
        User user = findUserById(userId);
        
        if (!StringUtils.hasText(user.getFcmToken())) {
            throw new BusinessException("FCM 토큰이 없습니다.", HttpStatus.BAD_REQUEST);
        }
        
        try {
            TopicManagementResponse response = FirebaseMessaging.getInstance()
                    .unsubscribeFromTopic(Collections.singletonList(user.getFcmToken()), topic);
            
            log.info("토픽 구독 해제 성공: userId={}, topic={}, successCount={}", 
                    userId, topic, response.getSuccessCount());
            
            // 구독 정보를 데이터베이스에서 비활성화
            Optional<TopicSubscription> subscription = topicSubscriptionRepository.findByUserAndTopic(user, topic);
            if (subscription.isPresent()) {
                subscription.get().unsubscribe();
                topicSubscriptionRepository.save(subscription.get());
            }
            
        } catch (FirebaseMessagingException e) {
            log.error("토픽 구독 해제 실패: userId={}, topic={}, error={}", userId, topic, e.getMessage(), e);
            throw new FcmException.TopicOperationException("토픽 구독 해제에 실패했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 여러 사용자를 특정 토픽에서 구독 해제합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public void unsubscribeMultipleFromTopic(List<Long> userIds, String topic) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        
        List<User> users = userRepository.findAllById(userIds);
        List<String> tokens = users.stream()
                .filter(user -> StringUtils.hasText(user.getFcmToken()))
                .map(User::getFcmToken)
                .collect(Collectors.toList());
        
        if (tokens.isEmpty()) {
            log.warn("구독 해제 대상 사용자들의 FCM 토큰이 없습니다.");
            return;
        }
        
        try {
            TopicManagementResponse response = FirebaseMessaging.getInstance()
                    .unsubscribeFromTopic(tokens, topic);
            
            log.info("다중 토픽 구독 해제 성공: topic={}, successCount={}, failureCount={}", 
                    topic, response.getSuccessCount(), response.getFailureCount());
            
        } catch (FirebaseMessagingException e) {
            log.error("다중 토픽 구독 해제 실패: topic={}, error={}", topic, e.getMessage(), e);
            throw new FcmException.TopicOperationException("토픽 구독 해제에 실패했습니다: " + e.getMessage());
        }
    }
    
    /**
     * FCM 토큰의 유효성을 검증합니다.
     */
    @Override
    public boolean validateToken(String fcmToken) {
        if (!StringUtils.hasText(fcmToken)) {
            return false;
        }
        
        // 토큰 형식 검증 (기본적인 길이 체크)
        if (fcmToken.length() < 100 || fcmToken.length() > 300) {
            return false;
        }
        
        // 실제 Firebase에 테스트 메시지를 보내보는 방법도 있지만,
        // 성능을 위해 여기서는 기본적인 검증만 수행
        return true;
    }
    
    /**
     * 사용자가 특정 토픽을 구독 중인지 확인합니다.
     * 
     * 참고: Firebase Admin SDK는 토픽 구독 상태를 직접 조회하는 API를 제공하지 않습니다.
     * 실제 구현에서는 별도의 데이터베이스 테이블에서 구독 정보를 관리해야 합니다.
     */
    @Override
    public boolean isSubscribedToTopic(Long userId, String topic) {
        return topicSubscriptionRepository.isUserSubscribedToTopic(userId, topic);
    }
    
    /**
     * 사용자가 구독 중인 모든 토픽 목록을 조회합니다.
     * 
     * 참고: Firebase Admin SDK는 사용자의 토픽 목록을 조회하는 API를 제공하지 않습니다.
     * 실제 구현에서는 별도의 데이터베이스 테이블에서 구독 정보를 관리해야 합니다.
     */
    @Override
    public List<String> getUserTopics(Long userId) {
        return topicSubscriptionRepository.findActiveTopicsByUserId(userId);
    }
    
    /**
     * 사용자 ID로 사용자를 조회합니다.
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }
}