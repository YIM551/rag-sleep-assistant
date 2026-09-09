package com.sleepwell.sleepwell_backend.exception;

import org.springframework.http.HttpStatus;

/**
 * 플랫폼 통합 관련 예외 클래스
 * 
 * Samsung Health, Apple Health, Google Fit 등 외부 플랫폼 연동 시 발생하는 예외를 처리합니다.
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
public class PlatformIntegrationException extends BusinessException {

    public PlatformIntegrationException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE, "PLATFORM_INTEGRATION_ERROR");
    }

    public PlatformIntegrationException(String message, String errorCode) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE, errorCode);
    }

    public PlatformIntegrationException(String message, HttpStatus status, String errorCode) {
        super(message, status, errorCode);
    }

    // Samsung Health 연동 실패
    public static PlatformIntegrationException samsungHealthConnectionFailed(String reason) {
        return new PlatformIntegrationException(
            "Samsung Health 연동에 실패했습니다: " + reason, 
            "SAMSUNG_HEALTH_CONNECTION_FAILED"
        );
    }

    // Apple Health 연동 실패
    public static PlatformIntegrationException appleHealthConnectionFailed(String reason) {
        return new PlatformIntegrationException(
            "Apple Health 연동에 실패했습니다: " + reason, 
            "APPLE_HEALTH_CONNECTION_FAILED"
        );
    }

    // Google Fit 연동 실패
    public static PlatformIntegrationException googleFitConnectionFailed(String reason) {
        return new PlatformIntegrationException(
            "Google Fit 연동에 실패했습니다: " + reason, 
            "GOOGLE_FIT_CONNECTION_FAILED"
        );
    }

    // 플랫폼 인증 만료
    public static PlatformIntegrationException platformAuthExpired(String platform) {
        return new PlatformIntegrationException(
            platform + " 플랫폼 인증이 만료되었습니다. 다시 연동해주세요", 
            HttpStatus.UNAUTHORIZED,
            "PLATFORM_AUTH_EXPIRED"
        );
    }

    // 플랫폼 권한 부족
    public static PlatformIntegrationException platformPermissionDenied(String platform, String permission) {
        return new PlatformIntegrationException(
            platform + " 플랫폼에서 " + permission + " 권한이 거부되었습니다", 
            HttpStatus.FORBIDDEN,
            "PLATFORM_PERMISSION_DENIED"
        );
    }

    // 플랫폼 데이터 형식 오류
    public static PlatformIntegrationException platformDataFormatError(String platform) {
        return new PlatformIntegrationException(
            platform + " 플랫폼의 데이터 형식이 올바르지 않습니다", 
            HttpStatus.BAD_REQUEST,
            "PLATFORM_DATA_FORMAT_ERROR"
        );
    }

    // 플랫폼 API 한도 초과
    public static PlatformIntegrationException platformRateLimitExceeded(String platform) {
        return new PlatformIntegrationException(
            platform + " 플랫폼의 API 호출 한도를 초과했습니다", 
            HttpStatus.TOO_MANY_REQUESTS,
            "PLATFORM_RATE_LIMIT_EXCEEDED"
        );
    }

    // 플랫폼 데이터 동기화 실패
    public static PlatformIntegrationException dataSyncFailed(String platform, String reason) {
        return new PlatformIntegrationException(
            platform + " 플랫폼 데이터 동기화에 실패했습니다: " + reason, 
            "PLATFORM_DATA_SYNC_FAILED"
        );
    }

    // 지원하지 않는 플랫폼
    public static PlatformIntegrationException unsupportedPlatform(String platform) {
        return new PlatformIntegrationException(
            "지원하지 않는 플랫폼입니다: " + platform, 
            HttpStatus.BAD_REQUEST,
            "UNSUPPORTED_PLATFORM"
        );
    }

    // 플랫폼 연결 해제 실패
    public static PlatformIntegrationException platformDisconnectionFailed(String platform) {
        return new PlatformIntegrationException(
            platform + " 플랫폼 연결 해제에 실패했습니다", 
            "PLATFORM_DISCONNECTION_FAILED"
        );
    }
}