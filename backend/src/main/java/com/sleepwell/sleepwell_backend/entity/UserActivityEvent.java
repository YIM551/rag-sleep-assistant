package com.sleepwell.sleepwell_backend.entity;

import com.sleepwell.sleepwell_backend.enums.ActivityEventType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import java.util.Map;

/**
 * 사용자 활동 이벤트 엔티티
 *
 * 사용자의 모든 활동 이벤트를 상세하게 기록합니다.
 * 로그인, AI 기능 사용, 화면 조회 등의 이벤트를 타임스탬프와 함께 저장하여
 * 사용자 행동 패턴 분석 및 통계 생성에 활용됩니다.
 *
 * 주요 용도:
 * - 사용자 행동 분석
 * - 기능 사용 통계
 * - 이벤트 히스토리 추적
 * - 개인화 추천 데이터 수집
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Entity
@Table(
    name = "user_activity_events",
    indexes = {
        @Index(name = "idx_user_event_type", columnList = "user_id,event_type"),
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_session_id", columnList = "session_id")
    }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UserActivityEvent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 이벤트를 발생시킨 사용자
     * 삭제된 사용자의 이벤트는 유지 (히스토리 분석용)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 이벤트 타입
     * LOGIN, AI_SLEEPY, AI_INSOMNIA 등
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private ActivityEventType eventType;

    /**
     * 이벤트 카테고리 (선택적)
     * AI 기능의 경우 세부 카테고리 (SLEEPY, INSOMNIA, SLEEP_TEST, STRESS, ACUPRESSURE)
     * 기타 이벤트의 경우 추가 분류 정보
     */
    @Column(name = "event_category", length = 100)
    private String eventCategory;

    /**
     * 이벤트 메타데이터 (JSON 형식)
     * 이벤트별 추가 정보를 유연하게 저장
     * 예: {"initialQuestion": "잠이 안 와요", "consultationType": "GENERAL"}
     */
    @Type(JsonType.class)
    @Column(name = "event_metadata", columnDefinition = "json")
    private Map<String, Object> eventMetadata;

    /**
     * 세션 ID
     * 동일 세션 내 이벤트 그룹핑 용도
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;

    /**
     * 클라이언트 IP 주소
     * 보안 및 지역별 분석 용도
     */
    @Column(name = "ip_address", length = 45) // IPv6 지원 (최대 45자)
    private String ipAddress;

    /**
     * User Agent 정보
     * 디바이스 및 브라우저 분석 용도
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * 디바이스 타입 (선택적)
     * MOBILE, TABLET, DESKTOP 등
     */
    @Column(name = "device_type", length = 20)
    private String deviceType;

    /**
     * 플랫폼 정보 (선택적)
     * IOS, ANDROID, WEB 등
     */
    @Column(name = "platform", length = 20)
    private String platform;

    // ===== 비즈니스 메서드 =====

    /**
     * 이벤트가 AI 기능 관련인지 확인
     *
     * @return AI 기능 이벤트인 경우 true
     */
    public boolean isAIFeatureEvent() {
        return eventType != null && eventType.isAIFeature();
    }

    /**
     * 이벤트가 인증 관련인지 확인
     *
     * @return 인증 이벤트인 경우 true
     */
    public boolean isAuthenticationEvent() {
        return eventType != null && eventType.isAuthentication();
    }

    /**
     * 메타데이터에 값 추가
     *
     * @param key 키
     * @param value 값
     */
    public void addMetadata(String key, Object value) {
        if (this.eventMetadata == null) {
            this.eventMetadata = new java.util.HashMap<>();
        }
        this.eventMetadata.put(key, value);
    }

    /**
     * 메타데이터에서 값 조회
     *
     * @param key 키
     * @return 값 (없으면 null)
     */
    public Object getMetadata(String key) {
        if (this.eventMetadata == null) {
            return null;
        }
        return this.eventMetadata.get(key);
    }
}
