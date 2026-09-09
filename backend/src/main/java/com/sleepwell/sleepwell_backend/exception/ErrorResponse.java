package com.sleepwell.sleepwell_backend.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * API 에러 응답 DTO
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // null이 아닌 필드만 JSON에 포함
public class ErrorResponse {

    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();

    private final int status;

    private final String error;

    private final String message;

    private final String path;

    private final String errorCode;

    /**
     * 상세 에러 정보 (유효성 검증 실패 시 필드별 에러)
     */
    private final Map<String, Object> details;
} 