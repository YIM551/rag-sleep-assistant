package com.sleepwell.sleepwell_backend.event;

import com.sleepwell.sleepwell_backend.enums.ActivityEventType;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * AI 기능 클릭 이벤트
 *
 * 사용자가 AI 기능(졸림, 불면, 수면검사, 스트레스, 경혈)을 클릭했을 때 발행되는 이벤트입니다.
 * PM 요구사항에 따라 5가지 AI 버튼 클릭을 추적하기 위해 설계되었습니다.
 *
 * 이벤트 흐름:
 * 1. ConsultationController 등에서 AI 기능 사용 시 발행
 * 2. UserActivityEventListener.onAIFeatureClick() 에서 수신
 * 3. UserActivityTrackingService.trackAIFeatureClick() 에서 비즈니스 로직 처리
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
public class AIFeatureClickEvent {

    /**
     * 사용자 ID
     */
    private final Long userId;

    /**
     * 사용자 이메일 (로깅용)
     */
    private final String userEmail;

    /**
     * 이벤트 타입
     * AI_SLEEPY, AI_INSOMNIA, AI_SLEEP_TEST, AI_STRESS, AI_ACUPRESSURE
     */
    private final ActivityEventType eventType;

    /**
     * 이벤트 카테고리
     * SLEEPY, INSOMNIA, SLEEP_TEST, STRESS, ACUPRESSURE
     */
    private final String category;

    /**
     * 이벤트 메타데이터
     * AI 기능 호출 시 추가 정보 (질문 내용, 상담 타입 등)
     */
    private final Map<String, Object> metadata;

    /**
     * 세션 ID (선택적)
     */
    private final String sessionId;

    /**
     * 이벤트 발생 시각
     */
    private final LocalDateTime timestamp;

    /**
     * 생성자 - 필수 정보만 포함
     *
     * @param userId 사용자 ID
     * @param userEmail 사용자 이메일
     * @param eventType 이벤트 타입
     * @param category 카테고리
     */
    public AIFeatureClickEvent(Long userId, String userEmail,
                               ActivityEventType eventType, String category) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.eventType = eventType;
        this.category = category;
        this.metadata = new HashMap<>();
        this.sessionId = null;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 생성자 - 메타데이터 포함
     *
     * @param userId 사용자 ID
     * @param userEmail 사용자 이메일
     * @param eventType 이벤트 타입
     * @param category 카테고리
     * @param metadata 메타데이터
     */
    public AIFeatureClickEvent(Long userId, String userEmail,
                               ActivityEventType eventType, String category,
                               Map<String, Object> metadata) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.eventType = eventType;
        this.category = category;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        this.sessionId = null;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 생성자 - 전체 정보 포함
     *
     * @param userId 사용자 ID
     * @param userEmail 사용자 이메일
     * @param eventType 이벤트 타입
     * @param category 카테고리
     * @param metadata 메타데이터
     * @param sessionId 세션 ID
     */
    public AIFeatureClickEvent(Long userId, String userEmail,
                               ActivityEventType eventType, String category,
                               Map<String, Object> metadata, String sessionId) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.eventType = eventType;
        this.category = category;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        this.sessionId = sessionId;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 카테고리 문자열을 ActivityEventType으로 변환하는 헬퍼 메서드
     *
     * @param categoryStr 카테고리 문자열 (SLEEPY, INSOMNIA, SLEEP_TEST, STRESS, ACUPRESSURE)
     * @return ActivityEventType
     */
    public static ActivityEventType mapCategoryToEventType(String categoryStr) {
        if (categoryStr == null) {
            return ActivityEventType.FEATURE_USE; // 기본값
        }

        switch (categoryStr.toUpperCase()) {
            case "SLEEPY":
                return ActivityEventType.AI_SLEEPY;
            case "INSOMNIA":
                return ActivityEventType.AI_INSOMNIA;
            case "SLEEP_TEST":
                return ActivityEventType.AI_SLEEP_TEST;
            case "STRESS":
                return ActivityEventType.AI_STRESS;
            case "ACUPRESSURE":
                return ActivityEventType.AI_ACUPRESSURE;
            default:
                return ActivityEventType.FEATURE_USE;
        }
    }

    /**
     * ConsultationTopic을 카테고리로 변환하는 헬퍼 메서드
     * (ConsultationController에서 사용)
     *
     * @param topic ConsultationTopic enum 값
     * @return 카테고리 문자열
     */
    public static String topicToCategory(Object topic) {
        if (topic == null) {
            return "GENERAL";
        }

        String topicStr = topic.toString().toUpperCase();

        // ConsultationTopic enum 값에 따라 매핑
        if (topicStr.contains("SLEEP")) {
            if (topicStr.contains("QUALITY")) {
                return "SLEEPY"; // 수면 품질 → 졸림
            } else if (topicStr.contains("DISORDER") || topicStr.contains("INSOMNIA")) {
                return "INSOMNIA"; // 수면 장애 → 불면
            } else {
                return "SLEEP_TEST"; // 기타 수면 관련 → 수면검사
            }
        } else if (topicStr.contains("STRESS") || topicStr.contains("ANXIETY")) {
            return "STRESS"; // 스트레스/불안
        } else if (topicStr.contains("ACUPRESSURE") || topicStr.contains("PRESSURE")) {
            return "ACUPRESSURE"; // 경혈
        }

        return "GENERAL";
    }

    /**
     * 이벤트 정보를 문자열로 반환 (로깅용)
     *
     * @return 이벤트 요약 정보
     */
    @Override
    public String toString() {
        return String.format("AIFeatureClickEvent[userId=%d, email=%s, type=%s, category=%s, time=%s]",
            userId, userEmail, eventType, category, timestamp);
    }
}
