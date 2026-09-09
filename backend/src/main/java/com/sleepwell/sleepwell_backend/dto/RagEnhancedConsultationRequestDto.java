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
 * RAG 강화 상담 요청 DTO
 *
 * 수면 의학 논문 기반 검색과 개인 수면 데이터를 결합한 상담을 위한 요청 데이터입니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "RAG 강화 상담 요청")
public class RagEnhancedConsultationRequestDto {

    @NotBlank(message = "질문은 필수입니다")
    @Size(min = 2, max = 1000, message = "질문은 2자 이상 1000자 이하여야 합니다")
    @Schema(description = "사용자 질문", example = "제 불면증을 개선할 수 있는 방법이 있을까요?")
    private String query;

    @Schema(description = "개인 수면 데이터 포함 여부 (기본값: true)", example = "true", defaultValue = "true")
    private Boolean includePersonalData;

    @Schema(description = "논문 근거 포함 여부 (기본값: true)", example = "true", defaultValue = "true")
    private Boolean includeCitations;

    @Schema(description = "분석할 수면 데이터 기간 (일) (기본값: 7)", example = "7", defaultValue = "7")
    private Integer sleepDataDays;

    @Schema(description = "검색할 논문 수 (기본값: 5)", example = "5", defaultValue = "5")
    private Integer topK;

    @Schema(description = "AI 응답 모드 (AUTO/FULL/PINGPONG, 기본값: AUTO)", example = "AUTO", defaultValue = "AUTO")
    private AiResponseMode responseMode;

    /**
     * 기본값 설정
     */
    public Boolean getIncludePersonalData() {
        return includePersonalData != null ? includePersonalData : true;
    }

    public Boolean getIncludeCitations() {
        return includeCitations != null ? includeCitations : true;
    }

    public Integer getSleepDataDays() {
        return sleepDataDays != null && sleepDataDays > 0 ? sleepDataDays : 7;
    }

    public Integer getTopK() {
        return topK != null && topK > 0 ? topK : 5;
    }

    public AiResponseMode getResponseMode() {
        return responseMode != null ? responseMode : AiResponseMode.AUTO;
    }
}
