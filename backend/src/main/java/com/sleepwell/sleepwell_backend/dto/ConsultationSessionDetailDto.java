package com.sleepwell.sleepwell_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sleepwell.sleepwell_backend.enums.ConsultationTopic;
import com.sleepwell.sleepwell_backend.enums.SessionStatus;
import com.sleepwell.sleepwell_backend.enums.SessionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 상담 세션 상세 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "상담 세션 상세 정보")
public class ConsultationSessionDetailDto {

    @Schema(description = "세션 ID", example = "123")
    private Long sessionId;

    @Schema(description = "상담 주제")
    private ConsultationTopic topic;

    @Schema(description = "세션 유형")
    private SessionType sessionType;

    @Schema(description = "세션 상태")
    private SessionStatus status;

    @Schema(description = "세션 제목", example = "AI 수면 상담 세션")
    private String title;

    @Schema(description = "초기 설명")
    private String initialDescription;

    @Schema(description = "세션 시작 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt;

    @Schema(description = "세션 종료 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endedAt;

    @Schema(description = "실제 소요 시간 (분)", example = "25")
    private Integer actualDurationMinutes;

    @Schema(description = "음성 상담 활성화 여부", example = "false")
    private Boolean voiceEnabled;

    @Schema(description = "메시지 수", example = "15")
    private Integer messageCount;

    @Schema(description = "메시지 목록")
    private List<MessageResponseDto> messages;

    @Schema(description = "사용자 만족도 점수", example = "4")
    private Integer userSatisfactionScore;

    @Schema(description = "AI 품질 점수", example = "85")
    private Integer aiQualityScore;

    @Schema(description = "사용자 피드백")
    private String userFeedback;

    @Schema(description = "메타데이터")
    private String metadata;
} 