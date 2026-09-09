package com.sleepwell.sleepwell_backend.exception;

import org.springframework.http.HttpStatus;

/**
 * API 호출 한도 초과 예외 클래스
 * 
 * Bucket4j Rate Limiting에서 발생하는 예외를 처리합니다.
 * 
 * @author SleepWell Development Team
 * @since 1.0
 */
public class RateLimitExceededException extends BusinessException {

    public RateLimitExceededException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED");
    }

    public RateLimitExceededException(String message, String errorCode) {
        super(message, HttpStatus.TOO_MANY_REQUESTS, errorCode);
    }

    // IP별 Rate Limit 초과
    public static RateLimitExceededException ipRateLimitExceeded(String ipAddress) {
        return new RateLimitExceededException(
            "IP 주소 " + ipAddress + "의 API 호출 한도를 초과했습니다", 
            "IP_RATE_LIMIT_EXCEEDED"
        );
    }

    // 사용자별 Rate Limit 초과
    public static RateLimitExceededException userRateLimitExceeded(String userId) {
        return new RateLimitExceededException(
            "사용자 " + userId + "의 API 호출 한도를 초과했습니다", 
            "USER_RATE_LIMIT_EXCEEDED"
        );
    }

    // 전역 Rate Limit 초과
    public static RateLimitExceededException globalRateLimitExceeded() {
        return new RateLimitExceededException(
            "시스템 전체 API 호출 한도를 초과했습니다. 잠시 후 다시 시도해주세요", 
            "GLOBAL_RATE_LIMIT_EXCEEDED"
        );
    }

    // 특정 엔드포인트 Rate Limit 초과
    public static RateLimitExceededException endpointRateLimitExceeded(String endpoint) {
        return new RateLimitExceededException(
            endpoint + " API의 호출 한도를 초과했습니다", 
            "ENDPOINT_RATE_LIMIT_EXCEEDED"
        );
    }
}