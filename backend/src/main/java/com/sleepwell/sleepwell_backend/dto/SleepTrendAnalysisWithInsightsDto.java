package com.sleepwell.sleepwell_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "수면 트렌드 분석 및 인사이트 통합 응답 DTO")
public class SleepTrendAnalysisWithInsightsDto {

    @Schema(description = "수면 오디오 이벤트 트렌드 분석 결과")
    private SleepAudioTrendAnalysisDto trendAnalysis;

    @Schema(description = "생성된 사용자 맞춤형 인사이트 목록")
    private List<InsightDto> insights;
} 