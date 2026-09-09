package com.sleepwell.sleepwell_backend.listeners;

import com.sleepwell.sleepwell_backend.entity.User;
import com.sleepwell.sleepwell_backend.event.UserLoginEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationEvents {

    private final AuditEventRepository auditEventRepository;
    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent success) {
        log.info("Login Success: {}", success.getAuthentication().getName());
        Map<String, Object> data = new HashMap<>();
        data.put("details", success.getAuthentication().getDetails());

        AuditEvent auditEvent = new AuditEvent(
                success.getAuthentication().getName(),
                "USER_LOGIN_SUCCESS",
                data
        );
        auditEventRepository.add(auditEvent);

        // 사용자 활동 추적 이벤트 발행
        try {
            Object principal = success.getAuthentication().getPrincipal();
            if (principal instanceof User) {
                User user = (User) principal;
                String sessionId = extractSessionId(success);
                String ipAddress = extractIpAddress(success);
                String userAgent = extractUserAgent(success);

                UserLoginEvent loginEvent = new UserLoginEvent(
                    user.getId(),
                    user.getEmail(),
                    sessionId,
                    ipAddress,
                    userAgent,
                    "NORMAL" // 일반 로그인
                );

                eventPublisher.publishEvent(loginEvent);
                log.debug("UserLoginEvent 발행 완료 - userId: {}", user.getId());
            }
        } catch (Exception e) {
            // 이벤트 발행 실패는 메인 로그인 프로세스에 영향을 주지 않음
            log.warn("UserLoginEvent 발행 실패: {}", e.getMessage());
        }
    }

    /**
     * 세션 ID 추출
     */
    private String extractSessionId(AuthenticationSuccessEvent success) {
        Object details = success.getAuthentication().getDetails();
        if (details instanceof WebAuthenticationDetails) {
            return ((WebAuthenticationDetails) details).getSessionId();
        }
        return null;
    }

    /**
     * IP 주소 추출
     */
    private String extractIpAddress(AuthenticationSuccessEvent success) {
        Object details = success.getAuthentication().getDetails();
        if (details instanceof WebAuthenticationDetails) {
            return ((WebAuthenticationDetails) details).getRemoteAddress();
        }
        return null;
    }

    /**
     * User Agent 추출 (선택적)
     */
    private String extractUserAgent(AuthenticationSuccessEvent success) {
        // WebAuthenticationDetails에는 User Agent가 없으므로 null 반환
        // 필요 시 Custom WebAuthenticationDetails 구현
        return null;
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent failures) {
        log.warn("Login Failure: {} - {}", failures.getAuthentication().getName(), failures.getException().getMessage());
        Map<String, Object> data = new HashMap<>();
        data.put("type", failures.getException().getClass().getName());
        data.put("message", failures.getException().getMessage());
        
        AuditEvent auditEvent = new AuditEvent(
                failures.getAuthentication().getName(),
                "USER_LOGIN_FAILURE",
                data
        );
        auditEventRepository.add(auditEvent);
    }
} 