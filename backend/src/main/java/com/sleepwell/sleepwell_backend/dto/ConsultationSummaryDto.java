package com.sleepwell.sleepwell_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 상담 요약 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "상담 요약")
public class ConsultationSummaryDto {

    @Schema(description = "요약 ID", example = "123")
    private Long id;

    @Schema(description = "세션 ID", example = "456")
    private Long sessionId;

    @Schema(description = "요약 내용")
    private String summary;

    @Schema(description = "핵심 인사이트")
    private String keyInsights;

    @Schema(description = "생성 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
} 