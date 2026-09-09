package com.sleepwell.sleepwell_backend.service;

import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.entity.UserActivityEvent;
import com.sleepwell.sleepwell_backend.entity.UserActivityStatistics;
import com.sleepwell.sleepwell_backend.enums.ActivityEventType;
import com.sleepwell.sleepwell_backend.event.AIFeatureClickEvent;
import com.sleepwell.sleepwell_backend.repository.UserActivityEventRepository;
import com.sleepwell.sleepwell_backend.repository.UserActivityStatisticsRepository;
import com.sleepwell.sleepwell_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 사용자 활동 추적 서비스
 *
 * 사용자의 로그인, AI 기능 사용 등의 활동을 추적하고 통계를 업데이트합니다.
 * 모든 추적 작업은 비동기 처리되며, 메인 비즈니스 로직과 독립적인 트랜잭션으로 실행됩니다.
 *
 * 주요 특징:
 * - REQUIRES_NEW 트랜잭션으로 메인 로직과 분리
 * - 예외 발생 시 로깅만 하고 메인 로직에 영향 없음
 * - 이벤트 상세 기록 + 통계 집계 동시 처리
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityTrackingService {

    private final UserActivityEventRepository eventRepository;
    private final UserActivityStatisticsRepository statisticsRepository;
    private final UserRepository userRepository;

    /**
     * 로그인 이벤트 추적
     *
     * 사용자 로그인 시 호출되어 이벤트를 기록하고 통계를 업데이트합니다.
     * 별도 트랜잭션으로 실행되어 메인 로그인 로직에 영향을 주지 않습니다.
     *
     * @param userId 사용자 ID
     * @param sessionId 세션 ID
     * @param ipAddress IP 주소
     * @param userAgent User Agent (선택적)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void trackLoginEvent(Long userId, String sessionId, String ipAddress, String userAgent) {
        try {
            log.debug("로그인 이벤트 추적 시작 - userId: {}, sessionId: {}", userId, sessionId);

            // 1. User 엔티티 조회
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

            // 2. 상세 이벤트 기록
            UserActivityEvent event = UserActivityEvent.builder()
                .user(user)
                .eventType(ActivityEventType.LOGIN)
                .sessionId(sessionId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

            eventRepository.save(event);
            log.debug("로그인 이벤트 저장 완료 - eventId: {}", event.getId());

            // 3. 통계 업데이트
            updateLoginStatistics(userId);

            log.debug("로그인 이벤트 추적 완료 - userId: {}", userId);

        } catch (Exception e) {
            // 추적 실패는 메인 로직에 영향을 주지 않도록 로깅만 수행
            log.error("로그인 이벤트 추적 실패 - userId: {}, error: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * 로그인 통계 업데이트
     *
     * @param userId 사용자 ID
     */
    private void updateLoginStatistics(Long userId) {
        // 재시도 로직으로 동시성 문제 해결 (최대 3번)
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                UserActivityStatistics statistics = statisticsRepository
                    .findByUserId(userId)
                    .orElseGet(() -> createNewStatistics(userId));

                statistics.incrementLogin();
                statisticsRepository.saveAndFlush(statistics);

                log.debug("로그인 통계 업데이트 완료 - userId: {}, loginCount: {}, attempt: {}",
                    userId, statistics.getLoginCount(), attempt);
                return; // 성공 시 종료

            } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
                if (attempt == maxRetries) {
                    log.warn("로그인 통계 업데이트 실패 (재시도 {} 회 초과) - userId: {}", maxRetries, userId);
                    throw e;
                }
                log.debug("로그인 통계 업데이트 재시도 ({}/{}) - userId: {}", attempt, maxRetries, userId);
                try {
                    Thread.sleep(50 * attempt); // 점진적 백오프
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * AI 기능 클릭 이벤트 추적
     *
     * 사용자가 AI 기능을 사용할 때 호출되어 이벤트를 기록하고 통계를 업데이트합니다.
     * 5가지 AI 기능(졸림, 불면, 수면검사, 스트레스, 경혈)을 추적합니다.
     *
     * @param userId 사용자 ID
     * @param eventType 이벤트 타입 (AI_SLEEPY, AI_INSOMNIA 등)
     * @param category 카테고리 (SLEEPY, INSOMNIA 등)
     * @param metadata 추가 메타데이터
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void trackAIFeatureClick(Long userId, ActivityEventType eventType,
                                    String category, Map<String, Object> metadata) {
        try {
            log.debug("AI 기능 클릭 이벤트 추적 시작 - userId: {}, type: {}, category: {}",
                userId, eventType, category);

            // 1. User 엔티티 조회
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

            // 2. 상세 이벤트 기록
            UserActivityEvent event = UserActivityEvent.builder()
                .user(user)
                .eventType(eventType)
                .eventCategory(category)
                .eventMetadata(metadata)
                .build();

            eventRepository.save(event);
            log.debug("AI 기능 이벤트 저장 완료 - eventId: {}", event.getId());

            // 3. 통계 업데이트
            updateAIFeatureStatistics(userId, category);

            log.debug("AI 기능 클릭 이벤트 추적 완료 - userId: {}, category: {}", userId, category);

        } catch (Exception e) {
            // 추적 실패는 메인 로직에 영향을 주지 않도록 로깅만 수행
            log.error("AI 기능 클릭 이벤트 추적 실패 - userId: {}, category: {}, error: {}",
                userId, category, e.getMessage(), e);
        }
    }

    /**
     * AI 기능 사용 통계 업데이트
     *
     * @param userId 사용자 ID
     * @param category AI 기능 카테고리
     */
    private void updateAIFeatureStatistics(Long userId, String category) {
        // 재시도 로직으로 동시성 문제 해결 (최대 3번)
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                UserActivityStatistics statistics = statisticsRepository
                    .findByUserId(userId)
                    .orElseGet(() -> createNewStatistics(userId));

                statistics.incrementAIClick(category);
                statisticsRepository.saveAndFlush(statistics);

                log.debug("AI 기능 통계 업데이트 완료 - userId: {}, category: {}, totalAICount: {}, attempt: {}",
                    userId, category, statistics.getTotalAIFeatureCount(), attempt);
                return; // 성공 시 종료

            } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
                if (attempt == maxRetries) {
                    log.warn("AI 기능 통계 업데이트 실패 (재시도 {} 회 초과) - userId: {}, category: {}",
                        maxRetries, userId, category);
                    throw e;
                }
                log.debug("AI 기능 통계 업데이트 재시도 ({}/{}) - userId: {}, category: {}",
                    attempt, maxRetries, userId, category);
                try {
                    Thread.sleep(50 * attempt); // 점진적 백오프
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * 새로운 사용자 통계 생성
     *
     * @param userId 사용자 ID
     * @return 생성된 UserActivityStatistics
     */
    private UserActivityStatistics createNewStatistics(Long userId) {
        log.debug("새로운 사용자 통계 생성 - userId: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        UserActivityStatistics statistics = UserActivityStatistics.builder()
            .userId(userId)
            .user(user)
            .build();

        return statisticsRepository.save(statistics);
    }

    /**
     * 일반 기능 사용 이벤트 추적 (확장용)
     *
     * 향후 추가 이벤트 추적이 필요할 때 사용할 수 있는 범용 메서드입니다.
     *
     * @param userId 사용자 ID
     * @param eventType 이벤트 타입
     * @param metadata 메타데이터
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void trackGenericEvent(Long userId, ActivityEventType eventType, Map<String, Object> metadata) {
        try {
            log.debug("일반 이벤트 추적 시작 - userId: {}, type: {}", userId, eventType);

            User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

            UserActivityEvent event = UserActivityEvent.builder()
                .user(user)
                .eventType(eventType)
                .eventMetadata(metadata)
                .build();

            eventRepository.save(event);
            log.debug("일반 이벤트 저장 완료 - eventId: {}, type: {}", event.getId(), eventType);

        } catch (Exception e) {
            log.error("일반 이벤트 추적 실패 - userId: {}, type: {}, error: {}",
                userId, eventType, e.getMessage(), e);
        }
    }

    /**
     * 사용자 활동 통계 조회
     *
     * @param userId 사용자 ID
     * @return 사용자 활동 통계 (없으면 null)
     */
    @Transactional(readOnly = true)
    public UserActivityStatistics getUserStatistics(Long userId) {
        return statisticsRepository.findByUserId(userId).orElse(null);
    }

    /**
     * 사용자가 AI 기능을 한 번이라도 사용했는지 확인
     *
     * @param userId 사용자 ID
     * @return AI 기능 사용 여부
     */
    @Transactional(readOnly = true)
    public boolean hasUsedAIFeature(Long userId) {
        UserActivityStatistics statistics = getUserStatistics(userId);
        return statistics != null && statistics.getTotalAIFeatureCount() > 0;
    }

    /**
     * 사용자의 총 로그인 횟수 조회
     *
     * @param userId 사용자 ID
     * @return 로그인 횟수 (통계가 없으면 0)
     */
    @Transactional(readOnly = true)
    public int getUserLoginCount(Long userId) {
        UserActivityStatistics statistics = getUserStatistics(userId);
        return statistics != null ? statistics.getLoginCount() : 0;
    }

    /**
     * 사용자의 총 AI 기능 사용 횟수 조회
     *
     * @param userId 사용자 ID
     * @return AI 기능 사용 횟수 (통계가 없으면 0)
     */
    @Transactional(readOnly = true)
    public int getUserTotalAIFeatureCount(Long userId) {
        UserActivityStatistics statistics = getUserStatistics(userId);
        return statistics != null ? statistics.getTotalAIFeatureCount() : 0;
    }
}
