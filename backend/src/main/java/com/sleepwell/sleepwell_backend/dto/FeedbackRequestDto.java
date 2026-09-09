package com.sleepwell.sleepwell_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 피드백 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "피드백 요청")
public class FeedbackRequestDto {

    @NotNull(message = "평점은 필수입니다")
    @Min(value = 1, message = "평점은 1 이상이어야 합니다")
    @Max(value = 5, message = "평점은 5 이하여야 합니다")
    @Schema(description = "상담 만족도 평점 (1-5)", example = "4")
    private Integer rating;

    @Size(max = 1000, message = "피드백은 1000자를 초과할 수 없습니다")
    @Schema(description = "상세 피드백")
    private String feedback;

    @Schema(description = "AI 품질 평가 (1-5)", example = "4")
    @Min(value = 1, message = "AI 품질 평가는 1 이상이어야 합니다")
    @Max(value = 5, message = "AI 품질 평가는 5 이하여야 합니다")
    private Integer aiQualityRating;

    @Schema(description = "응답 속도 평가 (1-5)", example = "5")
    @Min(value = 1, message = "응답 속도 평가는 1 이상이어야 합니다")
    @Max(value = 5, message = "응답 속도 평가는 5 이하여야 합니다")
    private Integer responseSpeedRating;

    @Schema(description = "개선 제안")
    @Size(max = 500, message = "개선 제안은 500자를 초과할 수 없습니다")
    private String improvementSuggestions;

    @Schema(description = "추천 의향 (1-5)", example = "4")
    @Min(value = 1, message = "추천 의향은 1 이상이어야 합니다")
    @Max(value = 5, message = "추천 의향은 5 이하여야 합니다")
    private Integer recommendationScore;
} 