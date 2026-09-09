package com.sleepwell.sleepwell_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 세션 종료 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "세션 종료 요청")
public class EndSessionRequestDto {

    @Schema(description = "종료 사유", example = "상담 완료")
    @Size(max = 500, message = "종료 사유는 500자를 초과할 수 없습니다")
    private String reason;

    @Schema(description = "사용자 만족도 (1-5)", example = "4")
    @Min(value = 1, message = "만족도는 1 이상이어야 합니다")
    @Max(value = 5, message = "만족도는 5 이하여야 합니다")
    private Integer userSatisfactionScore;

    @Schema(description = "사용자 피드백")
    @Size(max = 1000, message = "피드백은 1000자를 초과할 수 없습니다")
    private String userFeedback;

    @Schema(description = "요약 생성 요청 여부", example = "true")
    @Builder.Default
    private Boolean generateSummary = true;
} 