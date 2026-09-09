package com.sleepwell.sleepwell_backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * 비즈니스 로직 예외 클래스
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private final HttpStatus status;
    private final String errorCode;
    private final Map<String, String> details;
    private final ErrorCode error;
    
    // ErrorCode를 사용하는 생성자들
    public BusinessException(ErrorCode errorCode) {
        this(errorCode.getMessage(), errorCode.getHttpStatus(), errorCode.getCode(), null, errorCode, null);
    }
    
    public BusinessException(ErrorCode errorCode, String message) {
        this(message, errorCode.getHttpStatus(), errorCode.getCode(), null, errorCode, null);
    }
    
    public BusinessException(String message) {
        this(message, HttpStatus.BAD_REQUEST, "BUSINESS_ERROR", null, null, null);
    }
    
    public BusinessException(String message, HttpStatus status) {
        this(message, status, "BUSINESS_ERROR", null, null, null);
    }
    
    public BusinessException(String message, HttpStatus status, String errorCode) {
        this(message, status, errorCode, null, null, null);
    }

    public BusinessException(String message, HttpStatus status, String errorCode, Map<String, String> details) {
        this(message, status, errorCode, details, null, null);
    }
    
    public BusinessException(String message, Throwable cause) {
        this(message, HttpStatus.INTERNAL_SERVER_ERROR, "BUSINESS_ERROR", null, null, cause);
    }
    
    public BusinessException(String message, HttpStatus status, Throwable cause) {
        this(message, status, "BUSINESS_ERROR", null, null, cause);
    }
    
    public BusinessException(String message, HttpStatus status, String errorCode, Throwable cause) {
        this(message, status, errorCode, null, null, cause);
    }

    public BusinessException(String message, HttpStatus status, String errorCode, Map<String, String> details, Throwable cause) {
        this(message, status, errorCode, details, null, cause);
    }
    
    // 마스터 생성자 - 모든 필드를 초기화
    public BusinessException(String message, HttpStatus status, String errorCode, Map<String, String> details, ErrorCode error, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
        this.details = details;
        this.error = error;
    }
} 