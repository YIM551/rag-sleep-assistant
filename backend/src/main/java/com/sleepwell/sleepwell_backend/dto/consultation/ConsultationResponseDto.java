package com.sleepwell.sleepwell_backend.dto.consultation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 상담 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 상담 응답 데이터")
public class ConsultationResponseDto {

    @Schema(description = "응답 메시지", example = "수면 환경을 개선해보시는 것을 권장합니다...")
    private String response;

    @Schema(description = "사용된 AI 프로바이더", example = "gemini")
    private String provider;

    @Schema(description = "응답 생성 시간", example = "2025-01-13T10:30:00")
    private LocalDateTime timestamp;

    @Schema(description = "처리 시간 (밀리초)", example = "1500")
    private Long processingTimeMs;

    @Schema(description = "신뢰도 점수", example = "0.95")
    private Double confidenceScore;

    @Schema(description = "세션 ID", example = "session_123")
    private String sessionId;

    @Schema(description = "요청 ID", example = "req_123")
    private String requestId;

    @Schema(description = "성공 여부", example = "true")
    private Boolean success;

    @Schema(description = "오류 메시지")
    private String errorMessage;

    @Schema(description = "권장사항 목록")
    private List<String> recommendations;

    @Schema(description = "추가 메타데이터")
    private Map<String, Object> metadata;

    @Schema(description = "후속 질문")
    private List<String> followUpQuestions;

    @Schema(description = "상담 카테고리", example = "SLEEP_QUALITY")
    private String category;

    @Schema(description = "우선순위", example = "NORMAL")
    private String priority;
}