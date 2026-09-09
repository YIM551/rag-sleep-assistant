package com.sleepwell.sleepwell_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sleepwell.sleepwell_backend.enums.MessageStatus;
import com.sleepwell.sleepwell_backend.enums.MessageType;
import com.sleepwell.sleepwell_backend.enums.RecommendedFeature;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 메시지 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "메시지 응답")
public class MessageResponseDto {

    @Schema(description = "메시지 ID", example = "12345")
    private Long messageId;

    @Schema(description = "메시지 타입")
    private MessageType messageType;

    @Schema(description = "메시지 내용")
    private String content;

    @Schema(description = "음성 파일 경로")
    private String voiceFilePath;

    @Schema(description = "처리 상태")
    private MessageStatus status;

    @Schema(description = "AI 응답 내용")
    private String aiResponse;

    @Schema(description = "AI 음성 파일 경로")
    private String aiVoiceFilePath;

    @Schema(description = "처리 시간 (밀리초)", example = "1500")
    private Long responseTimeMs;

    @Schema(description = "메시지 전송 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sentAt;

    @Schema(description = "처리 완료 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processedAt;

    @Schema(description = "STT 결과")
    private String sttResult;

    @Schema(description = "STT 신뢰도 점수", example = "0.95")
    private Double sttConfidenceScore;

    @Schema(description = "감정 분석 결과")
    private String sentimentAnalysis;

    @Schema(description = "의도 인식 결과")
    private String intentRecognition;

    @Schema(description = "오류 메시지")
    private String errorMessage;

    @Schema(description = "AI 메시지 여부", example = "true")
    private Boolean isFromAI;

    @Schema(description = "메시지 타임스탬프 (별칭)")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    @Schema(description = "사용자 메시지 내용")
    private String userMessage;

    @Schema(description = "후속 질문 목록")
    private java.util.List<String> followUpQuestions;

    @Schema(description = "앱 내 추천 기능")
    private RecommendedFeature recommendedFeature;
} 
