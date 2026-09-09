package com.sleepwell.sleepwell_backend.dto;

import com.sleepwell.sleepwell_backend.enums.AiResponseMode;
import com.sleepwell.sleepwell_backend.enums.RecommendedFeature;
import com.sleepwell.sleepwell_backend.enums.SleepDataFetchStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * RAG 강화 상담 응답 DTO
 *
 * 논문 근거와 개인 수면 데이터를 결합한 AI 상담 응답입니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "RAG 강화 상담 응답")
public class RagEnhancedConsultationResponseDto {

    @Schema(description = "세션 ID (연속 대화용)", example = "123")
    private Long sessionId;

    @Schema(description = "AI 답변 (전체)", example = "당신의 최근 7일 수면 패턴을 보면...")
    private String answer;

    @Schema(description = "개인 수면 데이터 요약")
    private PersonalSleepSummary personalData;

    @Schema(description = "수면 데이터 조회 상태")
    private SleepDataFetchStatus sleepDataStatus;

    @Schema(description = "당신의 수면 상태 요약 섹션")
    private String sleepSummary;

    @Schema(description = "논문 인용 목록")
    private List<Citation> citations;

    @Schema(description = "권장 사항 목록")
    private List<String> recommendations;

    @Schema(description = "AI 응답 모드")
    private AiResponseMode aiResponseMode;

    @Schema(description = "PINGPONG 모드 후속 질문")
    private String followUpQuestion;

    @Schema(description = "후속 질문 목록")
    private List<String> followUpQuestions;

    @Schema(description = "앱 내 추천 기능")
    private RecommendedFeature recommendedFeature;

    @Schema(description = "처리 시간 (ms)")
    private Long processingTimeMs;

    /**
     * 개인 수면 데이터 요약
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "개인 수면 데이터 요약")
    public static class PersonalSleepSummary {

        @Schema(description = "분석 기간 (일)", example = "7")
        private Integer periodDays;

        @Schema(description = "평균 수면 시간 (시간)", example = "6.5")
        private Double avgSleepDuration;

        @Schema(description = "평균 수면 효율 (%)", example = "75.0")
        private Double avgSleepEfficiency;

        @Schema(description = "평균 야간 각성 횟수", example = "2.5")
        private Double avgWakeupCount;

        @Schema(description = "평균 잠들기까지 시간 (분)")
        private Double avgSleepLatencyMinutes;

        @Schema(description = "평균 취침 시간 (HH:mm)")
        private String avgBedTime;

        @Schema(description = "평균 기상 시간 (HH:mm)")
        private String avgWakeTime;

        @Schema(description = "총 수면 기록 수", example = "7")
        private Integer totalRecords;

        @Schema(description = "주요 패턴 또는 문제점", example = "입면 지연이 주 문제")
        private String keyPattern;

        @Schema(description = "추가 메타데이터")
        private Map<String, Object> metadata;
    }

    /**
     * 논문 인용
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "논문 인용 정보")
    public static class Citation {

        @Schema(description = "논문 제목", example = "AcupunctureImproves.pdf")
        private String title;

        @Schema(description = "인용구", example = "ISI score was 11.35 points in acupuncture group...")
        private String quote;

        @Schema(description = "관련성 설명", example = "당신의 입면 지연 문제에 직접 적용 가능")
        private String relevance;

        @Schema(description = "논문 URL (선택적)")
        private String url;
    }
}
