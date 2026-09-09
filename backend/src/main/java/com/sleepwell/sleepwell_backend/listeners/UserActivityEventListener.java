package com.sleepwell.sleepwell_backend.listeners;

import com.sleepwell.sleepwell_backend.event.AIFeatureClickEvent;
import com.sleepwell.sleepwell_backend.event.UserLoginEvent;
import com.sleepwell.sleepwell_backend.service.UserActivityTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 사용자 활동 이벤트 리스너
 *
 * 사용자 활동 관련 도메인 이벤트를 수신하여 비동기로 처리합니다.
 * 로그인, AI 기능 사용 등의 이벤트를 감지하고 UserActivityTrackingService를 호출합니다.
 *
 * 주요 특징:
 * - @Async로 비동기 처리 (메인 로직과 독립적으로 실행)
 * - taskExecutor 사용 (AsyncConfig에서 설정)
 * - 예외 발생 시에도 메인 로직에 영향 없음
 *
 * 처리 흐름:
 * 1. Controller/Service에서 이벤트 발행
 * 2. 이 리스너가 이벤트 수신
 * 3. 비동기로 UserActivityTrackingService 호출
 * 4. Service에서 별도 트랜잭션으로 DB 저장
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityEventListener {

    private final UserActivityTrackingService trackingService;

    /**
     * 사용자 로그인 이벤트 처리
     *
     * AuthenticationEvents에서 발행한 UserLoginEvent를 수신하여
     * 로그인 이벤트를 추적하고 통계를 업데이트합니다.
     *
     * @param event 로그인 이벤트
     */
    @EventListener
    @Async("taskExecutor")
    public void onUserLogin(UserLoginEvent event) {
        try {
            log.debug("로그인 이벤트 수신 - userId: {}, email: {}",
                event.getUserId(), event.getUserEmail());

            trackingService.trackLoginEvent(
                event.getUserId(),
                event.getSessionId(),
                event.getIpAddress(),
                event.getUserAgent()
            );

            log.debug("로그인 이벤트 처리 완료 - userId: {}", event.getUserId());

        } catch (Exception e) {
            // 비동기 처리 실패는 로깅만 수행
            // UserActivityTrackingService 내부에서도 예외 처리하지만,
            // 리스너 레벨에서도 방어적으로 처리
            log.error("로그인 이벤트 처리 중 예외 발생 - userId: {}, error: {}",
                event.getUserId(), e.getMessage(), e);
        }
    }

    /**
     * AI 기능 클릭 이벤트 처리
     *
     * ConsultationController 등에서 발행한 AIFeatureClickEvent를 수신하여
     * AI 기능 사용 이벤트를 추적하고 통계를 업데이트합니다.
     *
     * PM 요구사항:
     * - AI 버튼 클릭 빈도 추적 (졸림/잠안옴/수면검사/스트레스/경혈)
     *
     * @param event AI 기능 클릭 이벤트
     */
    @EventListener
    @Async("taskExecutor")
    public void onAIFeatureClick(AIFeatureClickEvent event) {
        try {
            log.debug("AI 기능 클릭 이벤트 수신 - userId: {}, type: {}, category: {}",
                event.getUserId(), event.getEventType(), event.getCategory());

            trackingService.trackAIFeatureClick(
                event.getUserId(),
                event.getEventType(),
                event.getCategory(),
                event.getMetadata()
            );

            log.debug("AI 기능 클릭 이벤트 처리 완료 - userId: {}, category: {}",
                event.getUserId(), event.getCategory());

        } catch (Exception e) {
            log.error("AI 기능 클릭 이벤트 처리 중 예외 발생 - userId: {}, category: {}, error: {}",
                event.getUserId(), event.getCategory(), e.getMessage(), e);
        }
    }

    /**
     * 이벤트 처리 통계 로깅 (선택적)
     *
     * 비동기 이벤트 처리가 완료된 후 호출되어 처리 통계를 로깅합니다.
     * 운영 환경에서는 warn/error 레벨 로그만 남기고,
     * 개발 환경에서는 debug 레벨로 상세 로그를 남깁니다.
     *
     * 향후 메트릭 수집을 위한 확장 포인트입니다.
     * (예: Micrometer를 사용한 이벤트 처리 카운터)
     */
    private void logEventProcessing(String eventType, Long userId, boolean success) {
        if (success) {
            log.debug("이벤트 처리 성공 - type: {}, userId: {}", eventType, userId);
        } else {
            log.warn("이벤트 처리 실패 - type: {}, userId: {}", eventType, userId);
        }
    }

    /**
     * 이벤트 처리 메트릭 기록 (향후 확장용)
     *
     * Micrometer를 사용하여 이벤트 처리 메트릭을 기록할 수 있습니다.
     * 현재는 주석 처리되어 있으며, 필요 시 활성화할 수 있습니다.
     *
     * 예시:
     * - sleepwell.events.processed{type=LOGIN, status=success} counter
     * - sleepwell.events.processed{type=AI_SLEEPY, status=success} counter
     */
    // private void recordEventMetric(String eventType, String status) {
    //     Counter.builder("sleepwell.events.processed")
    //         .description("처리된 사용자 활동 이벤트 수")
    //         .tag("type", eventType)
    //         .tag("status", status)
    //         .register(meterRegistry)
    //         .increment();
    // }
}
