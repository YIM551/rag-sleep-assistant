package com.sleepwell.sleepwell_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 세션 종료 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "세션 종료 응답")
public class SessionEndResponseDto {

    @Schema(description = "세션 ID")
    private Long sessionId;

    @Schema(description = "종료 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endedAt;

    @Schema(description = "총 소요 시간 (분)")
    private Integer totalDurationMinutes;

    @Schema(description = "총 메시지 수")
    private Integer totalMessages;

    @Schema(description = "요약 생성 여부")
    private Boolean summaryGenerated;

    @Schema(description = "요약 ID (생성된 경우)")
    private Long summaryId;

    @Schema(description = "종료 상태 메시지")
    private String message;
} 