package com.sleepwell.sleepwell_backend.event;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자 로그인 이벤트
 *
 * 사용자가 성공적으로 로그인했을 때 발행되는 도메인 이벤트입니다.
 * Spring의 ApplicationEvent 대신 POJO 이벤트를 사용하여
 * 비동기 처리와 트랜잭션 분리를 명확하게 합니다.
 *
 * 이벤트 흐름:
 * 1. AuthenticationEvents.onSuccess() 에서 발행
 * 2. UserActivityEventListener.onUserLogin() 에서 수신
 * 3. UserActivityTrackingService.trackLoginEvent() 에서 비즈니스 로직 처리
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Getter
public class UserLoginEvent {

    /**
     * 로그인한 사용자 ID
     */
    private final Long userId;

    /**
     * 사용자 이메일 (로깅용)
     */
    private final String userEmail;

    /**
     * 세션 ID
     * 동일 세션 내 이벤트 추적용
     */
    private final String sessionId;

    /**
     * 클라이언트 IP 주소
     */
    private final String ipAddress;

    /**
     * User Agent 정보
     */
    private final String userAgent;

    /**
     * 이벤트 발생 시각
     */
    private final LocalDateTime timestamp;

    /**
     * 로그인 타입 (선택적)
     * NORMAL, SOCIAL_GOOGLE, SOCIAL_KAKAO, SOCIAL_NAVER
     */
    private final String loginType;

    /**
     * 생성자 - 필수 정보만 포함
     *
     * @param userId 사용자 ID
     * @param userEmail 사용자 이메일
     * @param sessionId 세션 ID
     * @param ipAddress IP 주소
     */
    public UserLoginEvent(Long userId, String userEmail, String sessionId, String ipAddress) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.sessionId = sessionId;
        this.ipAddress = ipAddress;
        this.userAgent = null;
        this.timestamp = LocalDateTime.now();
        this.loginType = "NORMAL";
    }

    /**
     * 생성자 - 전체 정보 포함
     *
     * @param userId 사용자 ID
     * @param userEmail 사용자 이메일
     * @param sessionId 세션 ID
     * @param ipAddress IP 주소
     * @param userAgent User Agent
     * @param loginType 로그인 타입
     */
    public UserLoginEvent(Long userId, String userEmail, String sessionId, String ipAddress,
                          String userAgent, String loginType) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.sessionId = sessionId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.timestamp = LocalDateTime.now();
        this.loginType = loginType != null ? loginType : "NORMAL";
    }

    /**
     * 이벤트 정보를 문자열로 반환 (로깅용)
     *
     * @return 이벤트 요약 정보
     */
    @Override
    public String toString() {
        return String.format("UserLoginEvent[userId=%d, email=%s, ip=%s, time=%s]",
            userId, userEmail, ipAddress, timestamp);
    }
}
