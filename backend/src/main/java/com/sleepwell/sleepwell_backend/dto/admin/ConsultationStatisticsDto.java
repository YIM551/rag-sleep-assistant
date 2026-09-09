package com.sleepwell.sleepwell_backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationStatisticsDto {
    private LocalDate startDate;
    private LocalDate endDate;
    
    // 전체 통계
    private Long totalSessions;
    private Long uniqueUsers;
    private Long totalMessages;
    private Double averageMessagesPerSession;
    private Double averageSessionDuration;
    
    // AI 모델별 사용 통계
    private Map<String, ModelUsage> modelUsageStats;
    
    // 주제별 분포
    private Map<String, Integer> topicDistribution;
    
    // 시간대별 사용 패턴
    private Map<Integer, Integer> hourlyDistribution;
    
    // 주요 키워드 (빈도순)
    private List<KeywordFrequency> topKeywords;
    
    // 감정 분석
    private Map<String, Double> sentimentAnalysis;
    
    // 만족도
    private Double averageSatisfaction;
    private Map<String, Integer> satisfactionDistribution;
    
    // 비용 분석
    private Double totalEstimatedCost;
    private Map<String, Double> costByModel;
    
    // 일별 트렌드
    private List<DailyConsultationMetric> dailyMetrics;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelUsage {
        private String modelName;
        private Long sessionCount;
        private Long tokenCount;
        private Double estimatedCost;
        private Double averageResponseTime;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeywordFrequency {
        private String keyword;
        private Integer frequency;
        private Double percentage;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyConsultationMetric {
        private LocalDate date;
        private Long sessions;
        private Long messages;
        private Long uniqueUsers;
        private Double averageSatisfaction;
    }
}