package com.sleepwell.sleepwell_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * API 에러 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    /**
     * 오류 발생 시간
     */
    private LocalDateTime timestamp;
    
    /**
     * HTTP 상태 코드
     */
    private int status;
    
    /**
     * 오류 유형
     */
    private String error;
    
    /**
     * 오류 메시지
     */
    private String message;
    
    /**
     * 요청 경로
     */
    private String path;
    
    /**
     * 상세 오류 정보 (필드별 검증 오류 등)
     */
    private Map<String, String> details;
    
    /**
     * 에러 코드 (비즈니스 로직용)
     */
    private String errorCode;
} 