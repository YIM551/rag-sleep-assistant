package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.enums.ConsultationTopic;
import com.sleepwell.sleepwell_backend.enums.SessionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 상담 세션 시작 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "상담 세션 시작 요청")
public class StartConsultationRequestDto {

    @NotNull(message = "상담 주제는 필수입니다")
    @Schema(description = "상담 주제", example = "SLEEP_IMPROVEMENT")
    private ConsultationTopic topic;

    @Schema(description = "세션 유형", example = "GENERAL_CONSULTATION")
    @Builder.Default
    private SessionType sessionType = SessionType.GENERAL_CONSULTATION;

    @Size(max = 1000, message = "초기 메시지는 1000자를 초과할 수 없습니다")
    @Schema(description = "초기 상담 메시지", example = "밤을 새서 피곤해요.")
    private String initialMessage;

    @Schema(description = "우선순위", example = "NORMAL")
    private com.sleepwell.sleepwell_backend.enums.Priority priority;

    @Schema(description = "예상 소요 시간 (분)", example = "30")
    private Integer estimatedDurationMinutes;

    @Schema(description = "음성 상담 여부", example = "false")
    @Builder.Default
    private Boolean voiceEnabled = false;

    @Schema(description = "언어 설정", example = "ko")
    @Builder.Default
    private String language = "ko";
} 
