package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.enums.AiResponseMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG 상담 메시지 전송 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "RAG 상담 메시지 전송 요청")
public class SendRagMessageRequestDto {

    @NotBlank(message = "질문은 필수입니다")
    @Size(min = 2, max = 1000, message = "질문은 2자 이상 1000자 이하여야 합니다")
    @Schema(description = "사용자 질문", example = "제 불면증을 개선할 수 있는 방법이 있을까요?", required = true)
    private String query;

    @Schema(description = "AI 응답 모드 (AUTO/FULL/PINGPONG, 기본값: AUTO)", example = "AUTO", defaultValue = "AUTO")
    private AiResponseMode responseMode;

    @Schema(description = "분석할 수면 데이터 기간 (일) (기본값: 7)", example = "7", defaultValue = "7")
    private Integer sleepDataDays;

    public AiResponseMode getResponseMode() {
        return responseMode != null ? responseMode : AiResponseMode.AUTO;
    }

    public Integer getSleepDataDays() {
        return sleepDataDays != null && sleepDataDays > 0 ? sleepDataDays : 7;
    }
}
