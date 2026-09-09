package com.sleepwell.sleepwell_backend.aspect;

import com.sleepwell.sleepwell_backend.exception.BusinessException;
import com.sleepwell.sleepwell_backend.exception.ErrorCode;
import com.sleepwell.sleepwell_backend.security.CustomUserDetails;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ASMR Rate Limiting AOP Aspect
 *
 * 사용자별 세션 생성 및 API 호출에 대한 Rate Limiting을 적용합니다.
 * Resilience4j RateLimiter를 사용하여 구현됩니다.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ASMRRateLimitingAspect {

    private final RateLimiterRegistry rateLimiterRegistry;

    /**
     * Rate Limiting 어노테이션
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RateLimit {
        /**
         * Rate Limiter 인스턴스 이름
         */
        String name();

        /**
         * 키 추출 전략 (USER_ID, IP_ADDRESS, COMBINED)
         */
        KeyStrategy keyStrategy() default KeyStrategy.USER_ID;

        /**
         * Rate Limit 실패 시 사용할 에러 코드
         */
        ErrorCode errorCode() default ErrorCode.RATE_LIMIT_EXCEEDED;
    }

    /**
     * Rate Limiting 키 추출 전략
     */
    public enum KeyStrategy {
        USER_ID,      // 사용자 ID 기반
        IP_ADDRESS,   // IP 주소 기반
        COMBINED      // 사용자 ID + IP 주소 조합
    }

    /**
     * Rate Limiting 적용 포인트컷
     */
    @Around("@annotation(rateLimit)")
    public Object applyRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = extractKey(rateLimit.keyStrategy());
        String rateLimiterName = rateLimit.name();

        log.debug("Rate Limiting 적용: key={}, limiter={}", key, rateLimiterName);

        // Rate Limiter 인스턴스 가져오기
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(rateLimiterName);

        try {
            // Rate Limiting 적용하여 메서드 실행
            return rateLimiter.executeSupplier(() -> {
                try {
                    return joinPoint.proceed();
                } catch (Throwable throwable) {
                    if (throwable instanceof RuntimeException) {
                        throw (RuntimeException) throwable;
                    }
                    throw new RuntimeException(throwable);
                }
            });
        } catch (RequestNotPermitted e) {
            log.warn("Rate Limit 초과: key={}, limiter={}", key, rateLimiterName);
            throw new BusinessException(rateLimit.errorCode(),
                "요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    /**
     * Rate Limiting 키 추출
     */
    private String extractKey(KeyStrategy strategy) {
        switch (strategy) {
            case USER_ID:
                return extractUserId();
            case IP_ADDRESS:
                return extractIpAddress();
            case COMBINED:
                return extractUserId() + ":" + extractIpAddress();
            default:
                return "default";
        }
    }

    /**
     * 현재 사용자 ID 추출
     */
    private String extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof CustomUserDetails) {
                Long userId = ((CustomUserDetails) principal).getUser().getId();
                return "user:" + userId;
            }
        }

        // 인증되지 않은 사용자는 IP 기반으로 처리
        return "anonymous:" + extractIpAddress();
    }

    /**
     * 클라이언트 IP 주소 추출
     */
    private String extractIpAddress() {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            // 프록시를 통한 요청인 경우 실제 IP 추출
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }

            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }

            return request.getRemoteAddr();
        }

        return "unknown";
    }
}