package com.sleepwell.sleepwell_backend.exception;

import org.springframework.http.HttpStatus;

/**
 * FCM 관련 예외 클래스
 * Firebase Cloud Messaging 서비스 사용 중 발생하는 예외를 처리합니다.
 */
public class FcmException extends BusinessException {
    
    public FcmException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    public FcmException(String message, HttpStatus status) {
        super(message, status);
    }
    
    public FcmException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
    
    public FcmException(String message, HttpStatus status, Throwable cause) {
        super(message, status, cause);
    }
    
    /**
     * FCM 토큰이 유효하지 않을 때 발생하는 예외
     */
    public static class InvalidTokenException extends FcmException {
        public InvalidTokenException(String message) {
            super(message, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * FCM 서비스가 초기화되지 않았을 때 발생하는 예외
     */
    public static class NotInitializedException extends FcmException {
        public NotInitializedException(String message) {
            super(message, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
    
    /**
     * FCM 푸시 알림 발송이 실패했을 때 발생하는 예외
     */
    public static class SendFailedException extends FcmException {
        private final boolean retryable;
        private final boolean tokenInvalid;
        
        public SendFailedException(String message, boolean retryable, boolean tokenInvalid) {
            super(message, HttpStatus.INTERNAL_SERVER_ERROR);
            this.retryable = retryable;
            this.tokenInvalid = tokenInvalid;
        }
        
        public boolean isRetryable() {
            return retryable;
        }
        
        public boolean isTokenInvalid() {
            return tokenInvalid;
        }
    }
    
    /**
     * FCM 토픽 관련 작업이 실패했을 때 발생하는 예외
     */
    public static class TopicOperationException extends FcmException {
        public TopicOperationException(String message) {
            super(message, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}