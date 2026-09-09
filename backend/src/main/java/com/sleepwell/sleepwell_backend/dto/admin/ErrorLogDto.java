package com.sleepwell.sleepwell_backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorLogDto {
    private Long id;
    private LocalDateTime timestamp;
    private String level; // ERROR, WARN, FATAL
    private String errorType;
    private String message;
    private String stackTrace;
    
    // 컨텍스트 정보
    private Long userId;
    private String userEmail;
    private String requestUrl;
    private String requestMethod;
    private String userAgent;
    private String ipAddress;
    
    // 추가 메타데이터
    private Map<String, Object> metadata;
}