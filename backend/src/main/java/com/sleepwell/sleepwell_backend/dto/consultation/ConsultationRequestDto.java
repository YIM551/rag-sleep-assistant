package com.sleepwell.sleepwell_backend.dto.consultation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * AI 상담 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 상담 요청 데이터")
public class ConsultationRequestDto {

    @Schema(description = "상담 메시지", example = "최근 잠들기 어려워서 고민입니다.")
    @NotBlank(message = "상담 메시지는 필수입니다")
    @Size(max = 2000, message = "상담 메시지는 2000자를 초과할 수 없습니다")
    private String message;

    @Schema(description = "사용자 ID", example = "123")
    private Long userId;

    @Schema(description = "세션 ID", example = "session_123")
    private String sessionId;

    @Schema(description = "선호 AI 프로바이더", example = "gemini", allowableValues = {"openai", "anthropic", "gemini"})
    private String preferredProvider;

    @Schema(description = "상담 우선순위", example = "URGENT", allowableValues = {"LOW", "NORMAL", "HIGH", "URGENT"})
    private String priority;

    @Schema(description = "추가 컨텍스트 정보")
    private Map<String, Object> context;

    @Schema(description = "요청 ID", example = "req_123")
    private String requestId;
}