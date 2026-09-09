package com.sleepwell.sleepwell_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sleepwell.sleepwell_backend.enums.ConsultationTopic;
import com.sleepwell.sleepwell_backend.enums.SessionStatus;
import com.sleepwell.sleepwell_backend.enums.SessionType;
import com.sleepwell.sleepwell_backend.enums.SleepAnalysisReview;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 상담 세션 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "상담 세션 응답")
public class ConsultationSessionResponseDto {

    private static final List<String> DEFAULT_SUGGESTED_PROMPTS = List.of(
            "밤을 새서 피곤해요.",
            "커피는 언제 먹으면 잠이 깰까요?",
            "낮잠은 언제자면 좋을까요?"
    );

    public static List<String> defaultSuggestedPrompts() {
        return DEFAULT_SUGGESTED_PROMPTS;
    }

    @Schema(description = "세션 ID", example = "123")
    private Long sessionId;

    @Schema(description = "상담 주제")
    private ConsultationTopic topic;

    @Schema(description = "세션 유형")
    private SessionType sessionType;

    @Schema(description = "세션 상태")
    private SessionStatus status;

    @Schema(description = "세션 시작 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt;

    @Schema(description = "세션 종료 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endedAt;

    @Schema(description = "예상 소요 시간 (분)", example = "30")
    private Integer estimatedDurationMinutes;

    @Schema(description = "실제 소요 시간 (분)", example = "25")
    private Integer actualDurationMinutes;

    @Schema(description = "총 메시지 수", example = "15")
    private Integer totalMessages;

    @Schema(description = "읽지 않은 메시지 수", example = "2")
    private Integer unreadMessages;

    @Schema(description = "마지막 활성 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastActivityAt;

    @Schema(description = "AI 상담사 정보")
    private AiConsultantInfo aiConsultant;

    @Schema(description = "세션 품질 점수", example = "85")
    private Integer qualityScore;

    @Schema(description = "초기 메시지")
    private String initialMessage;

    @Schema(description = "음성 상담 활성화 여부", example = "false")
    @Builder.Default
    private Boolean voiceEnabled = false;

    @Schema(description = "수면 분석 리뷰 상태")
    private SleepAnalysisReview sleepAnalysisReview;

    @Schema(description = "추천 질문 목록")
    @Builder.Default
    private List<String> suggestedPrompts = DEFAULT_SUGGESTED_PROMPTS;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "AI 상담사 정보")
    public static class AiConsultantInfo {
        
        @Schema(description = "상담사 이름", example = "Dr. Sleep")
        private String name;
        
        @Schema(description = "전문 분야", example = "수면 전문")
        private String specialty;
        
        @Schema(description = "사용된 AI 모델", example = "GPT-4")
        private String aiModel;
        
        @Schema(description = "상담사 아바타 URL")
        private String avatarUrl;
    }
} 
