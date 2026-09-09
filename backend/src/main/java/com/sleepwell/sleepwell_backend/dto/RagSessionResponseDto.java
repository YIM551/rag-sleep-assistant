package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.enums.SessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * RAG 상담 세션 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "RAG 상담 세션 응답")
public class RagSessionResponseDto {

    @Schema(description = "세션 ID", example = "1")
    private Long sessionId;

    @Schema(description = "세션 상태", example = "IN_PROGRESS")
    private SessionStatus status;

    @Schema(description = "세션 생성 시간", example = "2025-11-27T09:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "개인 데이터 포함 여부", example = "true")
    private Boolean includePersonalData;

    @Schema(description = "논문 인용 포함 여부", example = "true")
    private Boolean includeCitations;

    @Schema(description = "수면 데이터 기간 (일)", example = "7")
    private Integer sleepDataDays;

    @Schema(description = "검색 논문 수", example = "5")
    private Integer topK;

    @Schema(description = "총 메시지 수", example = "0")
    private Integer totalMessages;
}
