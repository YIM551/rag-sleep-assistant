package com.sleepwell.sleepwell_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG 상담 세션 시작 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "RAG 상담 세션 시작 요청")
public class StartRagSessionRequestDto {

    @Schema(description = "개인 수면 데이터 포함 여부 (기본값: true)", example = "true", defaultValue = "true")
    private Boolean includePersonalData;

    @Schema(description = "논문 근거 포함 여부 (기본값: true)", example = "true", defaultValue = "true")
    private Boolean includeCitations;

    @Schema(description = "분석할 수면 데이터 기간 (일) (기본값: 7)", example = "7", defaultValue = "7")
    private Integer sleepDataDays;

    @Schema(description = "검색할 논문 수 (기본값: 5)", example = "5", defaultValue = "5")
    private Integer topK;

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
}
