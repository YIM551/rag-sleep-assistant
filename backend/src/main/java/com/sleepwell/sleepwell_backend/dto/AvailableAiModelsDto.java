package com.sleepwell.sleepwell_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 사용 가능한 AI 모델 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용 가능한 AI 모델 정보")
public class AvailableAiModelsDto {

    @Schema(description = "AI 모델 목록")
    private List<AiModelInfo> models;

    @Schema(description = "기본 모델")
    private String defaultModel;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "AI 모델 정보")
    public static class AiModelInfo {
        
        @Schema(description = "모델 ID", example = "gpt-4")
        private String modelId;

        @Schema(description = "모델 이름", example = "GPT-4")
        private String modelName;
        
        @Schema(description = "모델 설명")
        private String description;
        
        @Schema(description = "모델 기능")
        private List<String> capabilities;
        
        @Schema(description = "지원 언어")
        private List<String> supportedLanguages;
        
        @Schema(description = "음성 지원 여부")
        private Boolean voiceSupported;
        
        @Schema(description = "응답 속도 (1-5)")
        private Integer responseSpeed;
        
        @Schema(description = "정확도 (1-5)")
        private Integer accuracy;
        
        @Schema(description = "사용 가능 여부")
        private Boolean available;
    }
} 